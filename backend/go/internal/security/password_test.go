package security

import (
	"strings"
	"testing"
)

func TestPasswordHashing(t *testing.T) {
	password := "an example long password 🧥"
	first, err := HashPassword(password)
	if err != nil {
		t.Fatal(err)
	}
	second, err := HashPassword(password)
	if err != nil {
		t.Fatal(err)
	}
	if first == second || strings.Contains(first, password) {
		t.Fatal("password hashes need distinct salts")
	}
	if !VerifyPassword(password, first) || VerifyPassword("wrong long password", first) {
		t.Fatal("password verification failed")
	}
	for _, bad := range []string{
		"",
		"plaintext",
		strings.Replace(first, "m=19456", "m=999999999", 1),
		strings.Replace(first, "p=1", "p=0", 1),
		first + "$extra",
	} {
		if VerifyPassword(password, bad) {
			t.Fatal("malformed hash accepted")
		}
	}
}
