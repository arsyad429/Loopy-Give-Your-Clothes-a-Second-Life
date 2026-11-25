package httpapi

import (
	"context"
	"crypto/sha256"
	"errors"
	"io"
	"mime"
	"net/http"
	"strconv"
	"strings"
	"time"

	"loopy/backend/internal/domain"
	"loopy/backend/internal/vision"
)

const (
	maxCheckMultipart = vision.MaxCheckUpload + (64 << 10)
	checkTimeout      = 42 * time.Second
)

// ClothingCheckService coordinates photo upload validation, AI analysis, and private photo storage.
type ClothingCheckService struct {
	store  domain.ClothingCheckRepository
	vision domain.VisionAnalyzer
}

// NewClothingCheckService constructs a ClothingCheckService.
func NewClothingCheckService(store domain.ClothingCheckRepository, analyzer domain.VisionAnalyzer) *ClothingCheckService {
	return &ClothingCheckService{store: store, vision: analyzer}
}

func (s *Server) mountClothingCheckRoutes(mux *http.ServeMux) {
	// Bounding concurrent uploads also bounds the memory used by image decoders.
	slots := make(chan struct{}, 2)
	mux.Handle("POST /v1/checks", s.Authenticate(RequireRole("USER", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		key := r.Header.Values("Idempotency-Key")
		if len(key) != 1 || !domain.ClothingKeyPattern.MatchString(key[0]) {
			writeError(w, &domain.APIError{Status: 400, Code: "INVALID_IDEMPOTENCY_KEY", Message: "Sertakan satu Idempotency-Key yang valid."})
			return
		}

		select {
		case slots <- struct{}{}:
			defer func() { <-slots }()
		default:
			w.Header().Set("Retry-After", "2")
			writeError(w, &domain.APIError{Status: 429, Code: "CHECKS_BUSY", Message: "Pemeriksaan sedang penuh. Coba lagi."})
			return
		}

		upload, err := readClothingUpload(w, r)
		if err != nil {
			writeClothingCheckError(w, err)
			return
		}
		check, isNew, err := s.checks.create(r.Context(), currentPrincipal(r).User, key[0], upload)
		if err != nil {
			writeClothingCheckError(w, err)
			return
		}
		status := http.StatusOK
		if isNew {
			w.Header().Set("Location", "/v1/checks/"+check.ID)
			status = http.StatusCreated
		}
		writeJSON(w, status, check)
	}))))

	mux.Handle("GET /v1/checks/{id}", s.Authenticate(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id, valid := clothingCheckID(w, r)
		if !valid {
			return
		}
		check, err := s.checks.store.FindClothingCheck(r.Context(), currentPrincipal(r).User, id)
		if err != nil {
			writeClothingCheckError(w, err)
			return
		}
		writeJSON(w, http.StatusOK, check)
	})))

	mux.Handle("GET /v1/checks/{id}/photo", s.Authenticate(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id, valid := clothingCheckID(w, r)
		if !valid {
			return
		}
		photo, err := s.checks.store.FindClothingPhoto(r.Context(), currentPrincipal(r).User, id)
		if err != nil {
			writeClothingCheckError(w, err)
			return
		}
		w.Header().Set("Content-Type", vision.JPEGMIME)
		w.Header().Set("Content-Length", strconv.Itoa(len(photo)))
		_, _ = w.Write(photo)
	})))
}

func clothingCheckID(w http.ResponseWriter, r *http.Request) (string, bool) {
	id := r.PathValue("id")
	if !domain.UUIDPattern.MatchString(id) {
		writeError(w, &domain.APIError{Status: 400, Code: "INVALID_CHECK_ID", Message: "ID pemeriksaan tidak valid."})
		return "", false
	}
	return id, true
}

func readClothingUpload(w http.ResponseWriter, r *http.Request) (domain.ClothingUpload, error) {
	var upload domain.ClothingUpload
	contentType, _, err := mime.ParseMediaType(r.Header.Get("Content-Type"))
	if err != nil || contentType != "multipart/form-data" {
		return upload, &domain.APIError{Status: 415, Code: "MULTIPART_REQUIRED", Message: "Gunakan multipart/form-data."}
	}
	if r.ContentLength > maxCheckMultipart {
		return upload, vision.UploadTooLargeError()
	}
	r.Body = http.MaxBytesReader(w, r.Body, maxCheckMultipart)
	defer r.Body.Close()
	form, err := r.MultipartReader()
	if err != nil {
		return upload, invalidClothingUpload()
	}

	kindSeen := false
	for {
		part, err := form.NextRawPart()
		if errors.Is(err, io.EOF) {
			break
		}
		if err != nil {
			return domain.ClothingUpload{}, clothingUploadReadError(err)
		}
		switch {
		case part.FormName() == "clothingType" && part.FileName() == "" && !kindSeen:
			value, err := io.ReadAll(io.LimitReader(part, 129))
			if err != nil {
				return domain.ClothingUpload{}, clothingUploadReadError(err)
			}
			if len(value) > 128 {
				return domain.ClothingUpload{}, invalidClothingUpload()
			}
			upload.Kind = strings.TrimSpace(string(value))
			kindSeen = true

		case part.FormName() == "image" && part.FileName() != "" && upload.Data == nil:
			mediaType, _, err := mime.ParseMediaType(part.Header.Get("Content-Type"))
			if err != nil || (mediaType != vision.JPEGMIME && mediaType != vision.PNGMIME) {
				return domain.ClothingUpload{}, vision.UnsupportedClothingPhotoError()
			}
			data, err := io.ReadAll(io.LimitReader(part, vision.MaxCheckUpload+1))
			if err != nil {
				return domain.ClothingUpload{}, clothingUploadReadError(err)
			}
			if len(data) > vision.MaxCheckUpload {
				return domain.ClothingUpload{}, vision.UploadTooLargeError()
			}
			upload.Data, upload.MIME = data, mediaType

		default:
			return domain.ClothingUpload{}, invalidClothingUpload()
		}
	}
	// Account for any remaining bytes after the final multipart delimiter.
	if _, err := io.Copy(io.Discard, r.Body); err != nil {
		return domain.ClothingUpload{}, clothingUploadReadError(err)
	}
	if !kindSeen || upload.Data == nil {
		return domain.ClothingUpload{}, invalidClothingUpload()
	}
	if !domain.IsValidClothingType(upload.Kind) {
		return domain.ClothingUpload{}, &domain.APIError{Status: 400, Code: "INVALID_CLOTHING_TYPE", Message: "Jenis pakaian tidak dikenali."}
	}
	return upload, nil
}

func invalidClothingUpload() *domain.APIError {
	return &domain.APIError{Status: 400, Code: "INVALID_MULTIPART", Message: "Kirim satu image dan satu clothingType."}
}

func clothingUploadReadError(err error) error {
	var tooLarge *http.MaxBytesError
	if errors.As(err, &tooLarge) {
		return vision.UploadTooLargeError()
	}
	var timeout interface{ Timeout() bool }
	if errors.As(err, &timeout) && timeout.Timeout() {
		return &domain.APIError{Status: http.StatusRequestTimeout, Code: "UPLOAD_TIMEOUT", Message: "Waktu unggah foto habis."}
	}
	return invalidClothingUpload()
}

func (c *ClothingCheckService) create(ctx context.Context, actor domain.User, key string, upload domain.ClothingUpload) (domain.ClothingCheck, bool, error) {
	hash := sha256.New()
	hash.Write([]byte(upload.Kind + "\x00" + upload.MIME + "\x00"))
	hash.Write(upload.Data)
	claim, previous, err := c.store.ClaimClothingCheck(ctx, actor.ID, key, hash.Sum(nil))
	if err != nil {
		return domain.ClothingCheck{}, false, err
	}
	if previous != "" {
		check, err := c.store.FindClothingCheck(ctx, actor, previous)
		return check, false, err
	}

	saved := false
	defer func() {
		if !saved {
			releaseCtx, cancel := context.WithTimeout(context.WithoutCancel(ctx), 3*time.Second)
			defer cancel()
			_ = c.store.ReleaseClothingCheck(releaseCtx, claim) // A failed release expires automatically.
		}
	}()

	if c.vision == nil {
		return domain.ClothingCheck{}, false, &domain.APIError{Status: 503, Code: "AI_UNAVAILABLE", Message: "Layanan AI belum tersedia."}
	}
	jpeg, err := vision.NormalizeClothingPhoto(upload.Data, upload.MIME)
	if err != nil {
		return domain.ClothingCheck{}, false, err
	}
	result, err := c.analyze(ctx, jpeg, upload.Kind)
	if err != nil {
		return domain.ClothingCheck{}, false, err
	}
	check, err := c.store.CompleteClothingCheck(ctx, claim, upload.Kind, result, jpeg)
	saved = err == nil
	return check, saved, err
}

func (c *ClothingCheckService) analyze(ctx context.Context, jpeg []byte, kind string) (domain.VisionResult, error) {
	ctx, cancel := context.WithTimeout(ctx, 25*time.Second)
	defer cancel()

	result, err := c.vision.Analyze(ctx, jpeg, kind)
	var failure *vision.Failure
	if !errors.As(err, &failure) || !failure.RateLimited || failure.RetryAfter < 0 || failure.RetryAfter > 2 {
		return result, err
	}
	timer := time.NewTimer(time.Duration(failure.RetryAfter) * time.Second)
	defer timer.Stop()
	select {
	case <-ctx.Done():
		return domain.VisionResult{}, ctx.Err()
	case <-timer.C:
		return c.vision.Analyze(ctx, jpeg, kind)
	}
}

func writeClothingCheckError(w http.ResponseWriter, err error) {
	var failure *vision.Failure
	switch {
	case errors.Is(err, domain.ErrNotFound):
		err = &domain.APIError{Status: 404, Code: "CHECK_NOT_FOUND", Message: "Hasil pemeriksaan tidak ditemukan."}
	case errors.Is(err, domain.ErrCheckInProgress):
		w.Header().Set("Retry-After", "2")
		err = &domain.APIError{Status: 409, Code: "CHECK_IN_PROGRESS", Message: "Pemeriksaan masih berjalan."}
	case errors.Is(err, domain.ErrCheckKeyConflict):
		err = &domain.APIError{Status: 409, Code: "IDEMPOTENCY_CONFLICT", Message: "Key ini sudah digunakan untuk foto atau jenis pakaian berbeda."}
	case errors.Is(err, domain.ErrCheckClaimLost):
		err = &domain.APIError{Status: 409, Code: "CHECK_RESERVATION_EXPIRED", Message: "Reservasi pemeriksaan berakhir. Coba lagi dengan key yang sama."}
	case errors.Is(err, context.DeadlineExceeded):
		err = &domain.APIError{Status: 503, Code: "CHECK_UNAVAILABLE", Message: "Waktu pemeriksaan habis. Coba lagi."}
	case errors.As(err, &failure):
		if failure.Unavailable {
			if failure.RateLimited && failure.RetryAfter >= 0 {
				w.Header().Set("Retry-After", strconv.Itoa(failure.RetryAfter))
			}
			err = &domain.APIError{Status: 503, Code: "AI_UNAVAILABLE", Message: "Layanan AI belum tersedia. Coba lagi."}
		} else {
			err = &domain.APIError{Status: 502, Code: "AI_RESPONSE_FAILED", Message: "Pemeriksaan AI tidak dapat diselesaikan. Coba lagi."}
		}
	}
	writeError(w, err)
}
