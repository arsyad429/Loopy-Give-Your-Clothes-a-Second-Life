package main

import (
	"context"
	"crypto/sha256"
	"errors"
	"io"
	"mime"
	"net/http"
	"regexp"
	"strconv"
	"strings"
	"time"
)

const (
	maxCheckMultipart = maxCheckUpload + (64 << 10)
	checkTimeout      = 42 * time.Second
)

var clothingKeyPattern = regexp.MustCompile(`^[A-Za-z0-9._-]{1,128}$`)

var clothingTypes = map[string]bool{
	"TSHIRT": true, "SHIRT": true, "POLO": true, "BLOUSE": true, "TANK_TOP": true,
	"SWEATER": true, "HOODIE": true, "JACKET": true, "CARDIGAN": true,
	"LONG_PANTS": true, "SHORTS": true, "JEANS": true, "JOGGER": true, "SKIRT": true,
	"DRESS": true, "GAMIS": true, "TUNIC": true, "SPORTSWEAR": true, "SLEEPWEAR": true, "OTHER": true,
}

type clothingUpload struct {
	kind string
	mime string
	data []byte
}

type clothingCheckService struct {
	store  *pgStore
	vision visionAnalyzer
}

func (a *api) mountClothingCheckRoutes(mux *http.ServeMux) {
	// Bounding concurrent uploads also bounds the memory used by image decoders.
	slots := make(chan struct{}, 2)
	mux.Handle("POST /v1/checks", a.authenticate(requireRole("USER", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		key := r.Header.Values("Idempotency-Key")
		if len(key) != 1 || !clothingKeyPattern.MatchString(key[0]) {
			writeError(w, &apiError{Status: 400, Code: "INVALID_IDEMPOTENCY_KEY", Message: "Sertakan satu Idempotency-Key yang valid."})
			return
		}

		select {
		case slots <- struct{}{}:
			defer func() { <-slots }()
		default:
			w.Header().Set("Retry-After", "2")
			writeError(w, &apiError{Status: 429, Code: "CHECKS_BUSY", Message: "Pemeriksaan sedang penuh. Coba lagi."})
			return
		}

		upload, err := readClothingUpload(w, r)
		if err != nil {
			writeClothingCheckError(w, err)
			return
		}
		check, isNew, err := a.checks.create(r.Context(), currentPrincipal(r).User, key[0], upload)
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

	mux.Handle("GET /v1/checks/{id}", a.authenticate(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id, valid := clothingCheckID(w, r)
		if !valid {
			return
		}
		check, err := a.checks.store.findClothingCheck(r.Context(), currentPrincipal(r).User, id)
		if err != nil {
			writeClothingCheckError(w, err)
			return
		}
		writeJSON(w, http.StatusOK, check)
	})))

	mux.Handle("GET /v1/checks/{id}/photo", a.authenticate(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id, valid := clothingCheckID(w, r)
		if !valid {
			return
		}
		photo, err := a.checks.store.findClothingPhoto(r.Context(), currentPrincipal(r).User, id)
		if err != nil {
			writeClothingCheckError(w, err)
			return
		}
		w.Header().Set("Content-Type", jpegMIME)
		w.Header().Set("Content-Length", strconv.Itoa(len(photo)))
		_, _ = w.Write(photo)
	})))
}

func clothingCheckID(w http.ResponseWriter, r *http.Request) (string, bool) {
	id := r.PathValue("id")
	if !uuidPattern.MatchString(id) {
		writeError(w, &apiError{Status: 400, Code: "INVALID_CHECK_ID", Message: "ID pemeriksaan tidak valid."})
		return "", false
	}
	return id, true
}

func readClothingUpload(w http.ResponseWriter, r *http.Request) (clothingUpload, error) {
	var upload clothingUpload
	contentType, _, err := mime.ParseMediaType(r.Header.Get("Content-Type"))
	if err != nil || contentType != "multipart/form-data" {
		return upload, &apiError{Status: 415, Code: "MULTIPART_REQUIRED", Message: "Gunakan multipart/form-data."}
	}
	if r.ContentLength > maxCheckMultipart {
		return upload, uploadTooLarge()
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
			return clothingUpload{}, clothingUploadReadError(err)
		}
		switch {
		case part.FormName() == "clothingType" && part.FileName() == "" && !kindSeen:
			value, err := io.ReadAll(io.LimitReader(part, 129))
			if err != nil {
				return clothingUpload{}, clothingUploadReadError(err)
			}
			if len(value) > 128 {
				return clothingUpload{}, invalidClothingUpload()
			}
			upload.kind = strings.TrimSpace(string(value))
			kindSeen = true

		case part.FormName() == "image" && part.FileName() != "" && upload.data == nil:
			mediaType, _, err := mime.ParseMediaType(part.Header.Get("Content-Type"))
			if err != nil || (mediaType != jpegMIME && mediaType != pngMIME) {
				return clothingUpload{}, unsupportedClothingPhoto()
			}
			data, err := io.ReadAll(io.LimitReader(part, maxCheckUpload+1))
			if err != nil {
				return clothingUpload{}, clothingUploadReadError(err)
			}
			if len(data) > maxCheckUpload {
				return clothingUpload{}, uploadTooLarge()
			}
			upload.data, upload.mime = data, mediaType

		default:
			return clothingUpload{}, invalidClothingUpload()
		}
	}
	// Account for any remaining bytes after the final multipart delimiter.
	if _, err := io.Copy(io.Discard, r.Body); err != nil {
		return clothingUpload{}, clothingUploadReadError(err)
	}
	if !kindSeen || upload.data == nil {
		return clothingUpload{}, invalidClothingUpload()
	}
	if !clothingTypes[upload.kind] {
		return clothingUpload{}, &apiError{Status: 400, Code: "INVALID_CLOTHING_TYPE", Message: "Jenis pakaian tidak dikenali."}
	}
	return upload, nil
}

func invalidClothingUpload() *apiError {
	return &apiError{Status: 400, Code: "INVALID_MULTIPART", Message: "Kirim satu image dan satu clothingType."}
}

func clothingUploadReadError(err error) error {
	var tooLarge *http.MaxBytesError
	if errors.As(err, &tooLarge) {
		return uploadTooLarge()
	}
	var timeout interface{ Timeout() bool }
	if errors.As(err, &timeout) && timeout.Timeout() {
		return &apiError{Status: http.StatusRequestTimeout, Code: "UPLOAD_TIMEOUT", Message: "Waktu unggah foto habis."}
	}
	return invalidClothingUpload()
}

func (s *clothingCheckService) create(ctx context.Context, actor user, key string, upload clothingUpload) (clothingCheck, bool, error) {
	hash := sha256.New()
	hash.Write([]byte(upload.kind + "\x00" + upload.mime + "\x00"))
	hash.Write(upload.data)
	claim, previous, err := s.store.claimClothingCheck(ctx, actor.ID, key, hash.Sum(nil))
	if err != nil {
		return clothingCheck{}, false, err
	}
	if previous != "" {
		check, err := s.store.findClothingCheck(ctx, actor, previous)
		return check, false, err
	}

	saved := false
	defer func() {
		if !saved {
			releaseCtx, cancel := context.WithTimeout(context.WithoutCancel(ctx), 3*time.Second)
			defer cancel()
			_ = s.store.releaseClothingCheck(releaseCtx, claim) // A failed release expires automatically.
		}
	}()

	if s.vision == nil {
		return clothingCheck{}, false, &apiError{Status: 503, Code: "AI_UNAVAILABLE", Message: "Layanan AI belum tersedia."}
	}
	jpeg, err := normalizeClothingPhoto(upload.data, upload.mime)
	if err != nil {
		return clothingCheck{}, false, err
	}
	result, err := s.analyze(ctx, jpeg, upload.kind)
	if err != nil {
		return clothingCheck{}, false, err
	}
	check, err := s.store.completeClothingCheck(ctx, claim, upload.kind, result, jpeg)
	saved = err == nil
	return check, saved, err
}

func (s *clothingCheckService) analyze(ctx context.Context, jpeg []byte, kind string) (visionResult, error) {
	ctx, cancel := context.WithTimeout(ctx, 25*time.Second)
	defer cancel()

	result, err := s.vision.analyze(ctx, jpeg, kind)
	var failure *visionFailure
	if !errors.As(err, &failure) || !failure.rateLimited || failure.retryAfter < 0 || failure.retryAfter > 2 {
		return result, err
	}
	timer := time.NewTimer(time.Duration(failure.retryAfter) * time.Second)
	defer timer.Stop()
	select {
	case <-ctx.Done():
		return visionResult{}, ctx.Err()
	case <-timer.C:
		return s.vision.analyze(ctx, jpeg, kind)
	}
}

func writeClothingCheckError(w http.ResponseWriter, err error) {
	var vision *visionFailure
	switch {
	case errors.Is(err, errNotFound):
		err = &apiError{Status: 404, Code: "CHECK_NOT_FOUND", Message: "Hasil pemeriksaan tidak ditemukan."}
	case errors.Is(err, errCheckInProgress):
		w.Header().Set("Retry-After", "2")
		err = &apiError{Status: 409, Code: "CHECK_IN_PROGRESS", Message: "Pemeriksaan masih berjalan."}
	case errors.Is(err, errCheckKeyConflict):
		err = &apiError{Status: 409, Code: "IDEMPOTENCY_CONFLICT", Message: "Key ini sudah digunakan untuk foto atau jenis pakaian berbeda."}
	case errors.Is(err, errCheckClaimLost):
		err = &apiError{Status: 409, Code: "CHECK_RESERVATION_EXPIRED", Message: "Reservasi pemeriksaan berakhir. Coba lagi dengan key yang sama."}
	case errors.Is(err, context.DeadlineExceeded):
		err = &apiError{Status: 503, Code: "CHECK_UNAVAILABLE", Message: "Waktu pemeriksaan habis. Coba lagi."}
	case errors.As(err, &vision):
		if vision.unavailable {
			if vision.rateLimited && vision.retryAfter >= 0 {
				w.Header().Set("Retry-After", strconv.Itoa(vision.retryAfter))
			}
			err = &apiError{Status: 503, Code: "AI_UNAVAILABLE", Message: "Layanan AI belum tersedia. Coba lagi."}
		} else {
			err = &apiError{Status: 502, Code: "AI_RESPONSE_FAILED", Message: "Pemeriksaan AI tidak dapat diselesaikan. Coba lagi."}
		}
	}
	writeError(w, err)
}
