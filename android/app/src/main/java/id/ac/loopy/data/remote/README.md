# Lapisan HTTP Android

[Interface Retrofit](api/README.md) mendeklarasikan request dan respons. [DTO](dto/README.md) mengikuti kontrak JSON Go. `AppContainer` membuat Retrofit dan OkHttp bersama, lalu memberikan interface ke repository. Request berautentikasi memakai `AuthRepositoryImpl.withBearer`.

Folder [datasource](datasource/README.md) berisi kerangka pemisahan lapisan. Jalur aktif dari ViewModel ke Retrofit berada di [repository](../repository/README.md). [Kontrak server](../../../../../../../../../../docs/api-contract.md)
