package vision

import (
	"bytes"
	"encoding/binary"
	"errors"
	"image"
	"image/color"
	"image/jpeg"
	"image/png"
	"testing"

	"loopy/backend/internal/domain"
)

func TestNormalizeClothingPhoto(t *testing.T) {
	var input bytes.Buffer
	if err := png.Encode(&input, image.NewNRGBA(image.Rect(0, 0, 2000, 1000))); err != nil {
		t.Fatal(err)
	}
	jpg, err := NormalizeClothingPhoto(input.Bytes(), PNGMIME)
	if err != nil || len(jpg) > MaxVisionJPEG {
		t.Fatalf("JPEG length %d, error %v", len(jpg), err)
	}
	decoded, err := jpeg.Decode(bytes.NewReader(jpg))
	if err != nil || decoded.Bounds().Size() != image.Pt(1600, 800) {
		t.Fatalf("unexpected normalized dimensions, error %v", err)
	}
	r, g, b, _ := decoded.At(0, 0).RGBA()
	if r < 64000 || g < 64000 || b < 64000 {
		t.Error("transparent PNG should be flattened on white")
	}

	var problem *domain.APIError
	if _, err = NormalizeClothingPhoto(input.Bytes(), JPEGMIME); !errors.As(err, &problem) || problem.Status != 415 {
		t.Fatalf("expected mismatched MIME rejection, got %v", err)
	}
	if _, err = NormalizeClothingPhoto([]byte("broken"), JPEGMIME); !errors.As(err, &problem) || problem.Status != 422 {
		t.Fatalf("expected invalid photo rejection, got %v", err)
	}
}

func TestNormalizeJPEGHonorsEXIF(t *testing.T) {
	fixture := image.NewRGBA(image.Rect(0, 0, 12, 8))
	for y := range 8 {
		for x := range 12 {
			fixture.SetRGBA(x, y, color.RGBA{R: uint8(x * 22), A: 255})
		}
	}
	var input bytes.Buffer
	if err := jpeg.Encode(&input, fixture, nil); err != nil {
		t.Fatal(err)
	}
	// Little-endian TIFF with one SHORT field, EXIF orientation 6.
	exif := []byte{'E', 'x', 'i', 'f', 0, 0, 'I', 'I', 42, 0, 8, 0, 0, 0, 1, 0,
		0x12, 0x01, 3, 0, 1, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0}
	encoded := []byte{0xff, 0xd8, 0xff, 0xe1, 0, 0}
	binary.BigEndian.PutUint16(encoded[4:], uint16(len(exif)+2))
	encoded = append(append(encoded, exif...), input.Bytes()[2:]...)
	jpg, err := NormalizeClothingPhoto(encoded, JPEGMIME)
	if err != nil {
		t.Fatal(err)
	}
	cfg, err := jpeg.DecodeConfig(bytes.NewReader(jpg))
	if err != nil || cfg.Width != 8 || cfg.Height != 12 || jpegOrientation(jpg) != 1 {
		t.Fatalf("expected upright JPEG without EXIF, got %dx%d: %v", cfg.Width, cfg.Height, err)
	}
}
