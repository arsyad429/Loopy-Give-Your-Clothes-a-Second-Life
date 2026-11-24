package main

import (
	"bytes"
	"encoding/binary"
	"image"
	"image/draw"
	"image/jpeg"
	_ "image/png"
	"net/http"

	xdraw "golang.org/x/image/draw"
)

const (
	maxCheckUpload = 10 << 20
	maxVisionJPEG  = 2 << 20
	maxInputPixels = 50_000_000
	maxInputSide   = 12_000
	maxPhotoSide   = 1_600
	jpegMIME       = "image/jpeg"
	pngMIME        = "image/png"
)

func normalizeClothingPhoto(data []byte, mediaType string) ([]byte, error) {
	if len(data) > maxCheckUpload {
		return nil, uploadTooLarge()
	}
	config, format, err := image.DecodeConfig(bytes.NewReader(data))
	if err != nil {
		return nil, invalidClothingPhoto()
	}
	if (format != "jpeg" || mediaType != jpegMIME) && (format != "png" || mediaType != pngMIME) {
		return nil, unsupportedClothingPhoto()
	}
	if min(config.Width, config.Height) < 1 || max(config.Width, config.Height) > maxInputSide ||
		int64(config.Width)*int64(config.Height) > maxInputPixels {
		return nil, &apiError{
			Status: http.StatusUnprocessableEntity, Code: "INVALID_PHOTO_DIMENSIONS",
			Message: "Dimensi foto terlalu besar atau tidak valid.",
		}
	}
	source, decodedFormat, err := image.Decode(bytes.NewReader(data))
	if err != nil || decodedFormat != format || source.Bounds().Size() != image.Pt(config.Width, config.Height) {
		return nil, invalidClothingPhoto()
	}

	dstWidth, dstHeight := config.Width, config.Height
	if longest := max(dstWidth, dstHeight); longest > maxPhotoSide {
		dstWidth = max(1, dstWidth*maxPhotoSide/longest)
		dstHeight = max(1, dstHeight*maxPhotoSide/longest)
	}
	photo := image.NewRGBA(image.Rect(0, 0, dstWidth, dstHeight))
	draw.Draw(photo, photo.Bounds(), image.White, image.Point{}, draw.Src)
	xdraw.ApproxBiLinear.Scale(photo, photo.Bounds(), source, source.Bounds(), draw.Over, nil)
	if format == "jpeg" {
		photo = orientClothingPhoto(photo, jpegOrientation(data))
	}
	var encoded bytes.Buffer
	for _, quality := range []int{86, 72, 55} {
		encoded.Reset()
		if err := jpeg.Encode(&encoded, photo, &jpeg.Options{Quality: quality}); err != nil {
			return nil, err
		}
		if encoded.Len() <= maxVisionJPEG {
			return encoded.Bytes(), nil
		}
	}
	return nil, &apiError{
		Status: http.StatusUnprocessableEntity, Code: "PHOTO_CANNOT_BE_NORMALIZED",
		Message: "Foto belum bisa diproses. Gunakan foto yang lebih kecil.",
	}
}

func orientClothingPhoto(src *image.RGBA, orientation int) *image.RGBA {
	if orientation < 2 || orientation > 8 {
		return src
	}
	w, h := src.Bounds().Dx(), src.Bounds().Dy()
	bounds := src.Bounds()
	if orientation >= 5 {
		bounds = image.Rect(0, 0, h, w)
	}
	dst := image.NewRGBA(bounds)
	for y := range h {
		for x := range w {
			dx, dy := x, y
			switch orientation {
			case 2:
				dx = w - 1 - x
			case 3:
				dx, dy = w-1-x, h-1-y
			case 4:
				dy = h - 1 - y
			case 5:
				dx, dy = y, x
			case 6:
				dx, dy = h-1-y, x
			case 7:
				dx, dy = h-1-y, w-1-x
			case 8:
				dx, dy = y, w-1-x
			}
			dst.SetRGBA(dx, dy, src.RGBAAt(x, y))
		}
	}
	return dst
}

// jpegOrientation reads only the EXIF orientation in the first TIFF directory.
func jpegOrientation(data []byte) int {
	if len(data) < 2 || data[0] != 0xff || data[1] != 0xd8 {
		return 1
	}
	for remaining := data[2:]; len(remaining) > 3 && remaining[0] == 0xff; {
		// Consecutive 0xff bytes before a marker are padding.
		remaining = remaining[1:]
		if remaining[0] == 0xff {
			continue
		}
		marker := remaining[0]
		if marker == 0xda || marker == 0xd9 {
			break
		}
		n := int(binary.BigEndian.Uint16(remaining[1:3]))
		if n < 2 || n+1 > len(remaining) {
			break
		}
		payload := remaining[3 : n+1]
		if marker == 0xe1 && bytes.HasPrefix(payload, []byte("Exif\x00\x00")) {
			return tiffOrientation(payload[6:])
		}
		remaining = remaining[n+1:]
	}
	return 1
}

func tiffOrientation(data []byte) int {
	if len(data) < 8 {
		return 1
	}
	var order binary.ByteOrder
	switch string(data[:2]) {
	case "II":
		order = binary.LittleEndian
	case "MM":
		order = binary.BigEndian
	default:
		return 1
	}
	offset := int64(order.Uint32(data[4:8]))
	if order.Uint16(data[2:4]) != 42 || offset < 8 || offset > int64(len(data))-2 {
		return 1
	}
	directory := data[offset:]
	count := int(order.Uint16(directory[:2]))
	for entry := directory[2:]; count > 0 && len(entry) >= 12; entry, count = entry[12:], count-1 {
		if order.Uint16(entry[:2]) == 0x0112 && order.Uint16(entry[2:4]) == 3 && order.Uint32(entry[4:8]) == 1 {
			if value := int(order.Uint16(entry[8:10])); 1 <= value && value <= 8 {
				return value
			}
		}
	}
	return 1
}

func uploadTooLarge() *apiError {
	return &apiError{Status: http.StatusRequestEntityTooLarge, Code: "PHOTO_TOO_LARGE", Message: "Ukuran foto maksimal 10 MiB."}
}

func invalidClothingPhoto() *apiError {
	return &apiError{Status: http.StatusUnprocessableEntity, Code: "INVALID_PHOTO", Message: "Foto tidak bisa dibaca."}
}

func unsupportedClothingPhoto() *apiError {
	return &apiError{Status: http.StatusUnsupportedMediaType, Code: "UNSUPPORTED_PHOTO", Message: "Gunakan file JPEG atau PNG yang valid."}
}
