package vision

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func visionCompletion(t *testing.T, completion, finish string) []byte {
	t.Helper()
	data, err := json.Marshal(map[string]any{
		"choices": []any{map[string]any{
			"finish_reason": finish,
			"message":       map[string]string{"content": completion},
		}},
	})
	if err != nil {
		t.Fatal(err)
	}
	return data
}

func TestGroqVisionRequest(t *testing.T) {
	for _, mode := range []string{"json_schema", "json_object"} {
		t.Run(mode, func(t *testing.T) {
			jpeg := []byte{0xff, 0xd8, 0xff}
			reply := visionCompletion(t, `{"aiResult":"NOT_RECOMMENDED","aiDescription":"Kain robek.","actionRecommendation":"Perbaiki sobekan sebelum donasi."}`, "stop")
			server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				if r.Method != http.MethodPost || r.Header.Get("Authorization") != "Bearer local-fixture" {
					t.Error("expected authenticated POST to Groq")
				}
				var payload struct {
					Model  string                               `json:"model"`
					Format map[string]any                       `json:"response_format"`
					Effort string                               `json:"reasoning_effort"`
					Msgs   []struct{ Content []map[string]any } `json:"messages"`
				}
				if err := json.NewDecoder(r.Body).Decode(&payload); err != nil {
					t.Error(err)
					return
				}
				if payload.Model != DefaultGroqModel || payload.Format["type"] != mode || payload.Effort != "none" {
					t.Error("unexpected Groq model or response mode")
				}
				if len(payload.Msgs) != 1 || len(payload.Msgs[0].Content) != 2 {
					t.Error("expected one image and one prompt")
					return
				}
				parts := payload.Msgs[0].Content
				imageURL, _ := parts[1]["image_url"].(map[string]any)
				if imageURL["url"] != "data:image/jpeg;base64,"+base64.StdEncoding.EncodeToString(jpeg) {
					t.Error("expected the normalized photo in the request")
				}
				if prompt, _ := parts[0]["text"].(string); !strings.Contains(prompt, "TSHIRT") {
					t.Error("expected clothing type in prompt")
				}
				_, _ = w.Write(reply)
			}))
			defer server.Close()
			gateway := GroqClient{Client: server.Client(), URL: server.URL, Key: "local-fixture", Model: DefaultGroqModel, Mode: mode}
			result, err := gateway.Analyze(context.Background(), jpeg, "TSHIRT")
			if err != nil || result.Category != "NOT_RECOMMENDED" || result.Recommendation == nil {
				t.Fatalf("result=%+v error=%v", result, err)
			}
		})
	}
}

func TestVisionRejectsMalformedOutput(t *testing.T) {
	for _, test := range []struct{ name, result, finish string }{
		{"unknown category", `{"aiResult":"UNKNOWN","aiDescription":"x","actionRecommendation":null}`, "stop"},
		{"missing recommendation", `{"aiResult":"NOT_RECOMMENDED","aiDescription":"x","actionRecommendation":null}`, "stop"},
		{"description has wrong type", `{"aiResult":"NEEDS_RECHECK","aiDescription":2,"actionRecommendation":null}`, "stop"},
		{"truncated", `{"aiResult":"NEEDS_RECHECK","aiDescription":"x","actionRecommendation":null}`, "length"},
		{"invalid json", "{", "stop"},
	} {
		t.Run(test.name, func(t *testing.T) {
			var failure *Failure
			_, err := decodeVisionResult(visionCompletion(t, test.result, test.finish))
			if !errors.As(err, &failure) || failure.Unavailable {
				t.Fatalf("expected invalid-reply failure, got %v", err)
			}
		})
	}
}

func TestGroqRateLimit(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Retry-After", "2")
		w.WriteHeader(http.StatusTooManyRequests)
	}))
	defer server.Close()
	gateway := GroqClient{Client: server.Client(), URL: server.URL, Key: "local-fixture", Model: DefaultGroqModel}
	var failure *Failure
	_, err := gateway.Analyze(context.Background(), nil, "TSHIRT")
	if !errors.As(err, &failure) || !failure.RateLimited || failure.RetryAfter != 2 {
		t.Fatalf("unexpected rate limit error: %+v", failure)
	}
}
