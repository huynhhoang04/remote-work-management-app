# Plan App 01 — React Native phase 1 (Expo + TypeScript)

> Đồ án môn học — app mobile phase 1: đăng nhập & quản lý dự án cơ bản, nối backend đã deploy
> (`https://remoteapp-backend.onrender.com`).
> **Quyết định của nhóm (thay plan gốc Kotlin/Java-native):** dùng **React Native (Expo, TypeScript)**
> vì nhóm quen React, cần UI/animation phong phú (Render free chậm → cần trạng thái chờ mượt) và
> không muốn dùng Kotlin. App **vẫn build ra Android** (APK) — ghi rõ lý do chuyển đổi trong README.
> Stack UI: **React Native Paper (Material 3)**.

---

## 1. Phạm vi phase 1

| Nhóm | Chức năng | API dùng |
|---|---|---|
| Auth | Đăng ký, đăng nhập, tự đăng nhập lại, đăng xuất | `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout` |
| Auth | Quên mật khẩu, đổi mật khẩu trong hồ sơ | `POST /auth/forgot-password`, `/users/me/password` |
| Hồ sơ | Xem/cập nhật tên hiển thị | `GET/PATCH /users/me` |
| Dự án | Danh sách, tạo, xem chi tiết | `GET/POST /projects`, `GET /projects/{id}` |
| Thành viên | Danh sách thành viên | `GET /projects/{id}/members` |
| Link mời | Xem/sinh/thu hồi link mời, tham gia bằng mã | `POST/GET/DELETE /projects/{id}/invites`, `POST /projects/join` |
| Phân quyền UI | Ẩn/hiện hành động theo vai trò (`OWNER/ADMIN/MEMBER/VIEWER`) | backend đã enforce |

**Không làm phase 1:** task/Kanban/timeline, thống kê, FCM/push, chat, file, lịch, offline cache,
deep-link reset mật khẩu, i18n đa ngôn ngữ (chỉ tiếng Việt).

---

## 2. Quyết định kỹ thuật

| Hạng mục | Quyết định | Ghi chú |
|---|---|---|
| Framework | **Expo SDK mới nhất (managed)** | `npx create-expo-app`; không native code |
| Ngôn ngữ | **TypeScript** (strict) | type API/DTO, zod validate |
| Điều hướng | **Expo Router** (file-based) | route = khai báo luồng màn hình |
| UI kit | **React Native Paper (M3)** + theme tuỳ biến | component sẵn: Button/TextInput/Card/Chip/FAB/Snackbar/Dialog |
| Server state | **TanStack Query v5** | cache, retry, loading/error chuẩn hoá |
| HTTP | **axios** + interceptor | refresh token single-flight |
| Form + validate | **react-hook-form + zod** | schema khớp luật backend (email, password ≥ 8…) |
| Lưu token | **expo-secure-store** | Keychain/Keystore |
| Animation | **react-native-reanimated** (có sẵn trong Expo) | skeleton, fade/slide, banner "server đang khởi động" |
| Icons / font | `@expo/vector-icons`, `expo-font` | MaterialCommunityIcons |
| Ngày giờ | `dayjs` | format `dd/MM/yyyy` |
| Test | `jest-expo` + **@testing-library/react-native** | unit + component |
| Build APK | **EAS Build** (free) hoặc `npx expo run:android` | Android Studio đã có trên máy |
| Base URL | `EXPO_PUBLIC_API_URL` (`.env`) | dev có thể trỏ `http://<LAN-IP>:8080` |

---

## 3. Cấu trúc project

```
mobile/                                  # Expo app (commit vào repo)
├─ src/
│  ├─ app/                               # Expo Router — route = màn hình
│  │  ├─ _layout.tsx                     # Providers: Paper, QueryClient, Session, Snackbar
│  │  ├─ index.tsx                       # splash/redirect theo session
│  │  ├─ (auth)/_layout.tsx
│  │  ├─ (auth)/login.tsx
│  │  ├─ (auth)/register.tsx
│  │  ├─ (auth)/forgot-password.tsx
│  │  ├─ (app)/_layout.tsx               # stack sau đăng nhập (header + back)
│  │  ├─ (app)/projects/index.tsx        # danh sách dự án
│  │  ├─ (app)/projects/new.tsx          # tạo dự án
│  │  ├─ (app)/projects/[id].tsx         # chi tiết: mô tả, vai trò, link mời, thành viên
│  │  ├─ (app)/projects/join.tsx         # tham gia bằng mã
│  │  └─ (app)/profile.tsx               # hồ sơ + đổi mật khẩu + đăng xuất
│  ├─ api/            client.ts (axios + interceptors), auth.ts, users.ts, projects.ts,
│  │                  errors.ts, tokenStore.ts, activity.ts
│  ├─ types/          api.ts (User, Project, Member, Invite, TokenResponse, ApiError)
│  ├─ schemas/        auth.ts, project.ts (zod)
│  ├─ store/          session.tsx (Context), queryClient.ts
│  ├─ features/       auth/hooks.ts, projects/hooks.ts, profile/hooks.ts
│  ├─ components/     AppButton, ControlledField, FormField, StateView, ProjectCard, MemberRow,
│  │                  RoleChip, InviteCard, SkeletonList, SnackbarProvider, ServerWakeBanner
│  ├─ theme/          tokens.ts, paperTheme.ts
│  └─ utils/          constants.ts (text VI), format.ts, zodResolver.ts
├─ app.json                    # name "RemoteApp", slug remoteapp, scheme remoteapp,
│                              # android.package com.example.remoteapp
├─ eas.json                    # (thêm khi build APK bằng EAS) profile preview/production
├─ tsconfig.json · babel.config.js · package.json · jest.setup.js
├─ .env.example                # EXPO_PUBLIC_API_URL=https://remoteapp-backend.onrender.com
└─ .gitignore                  # .expo/, node_modules/, .env
```

**Dependency chính:** `expo`, `expo-router`, `react-native-paper`, `react-native-safe-area-context`,
`@tanstack/react-query`, `axios`, `zod`, `react-hook-form`, `expo-secure-store`, `expo-clipboard`,
`react-native-reanimated`, `dayjs`, `@expo/vector-icons`; dev: `typescript`, `jest-expo`,
`@testing-library/react-native`, `test-renderer`, `eslint-config-expo`, `prettier`.

---

## 4. Design system (khai báo trước khi làm màn hình)

### 4.1 Tokens (`src/theme/tokens.ts`)

| Nhóm | Giá trị |
|---|---|
| `colors.primary` | `#3F51B5` (light) / `#AAB6FF` (dark) |
| `colors.onPrimary` | `#FFFFFF` / `#1A237E` |
| `colors.surface` / `surfaceVariant` | `#FFFFFF` / `#F4F2F7` · dark `#121212` / `#1E1E24` |
| `colors.onSurface` / `onSurfaceVariant` | `#1C1B1F` / `#49454F` · dark `#E6E1E5` / `#CAC4D0` |
| `colors.error` / `success` | `#B3261E` / `#2E7D32` |
| `roleColors` | OWNER `#EF6C00`, ADMIN `#6A1B9A`, MEMBER `#0277BD`, VIEWER `#546E7A` |
| `spacing` | 4 / 8 / 12 / 16 / 24 / 32 |
| `radius` | sm 8, md 12, lg 16, pill 999 |
| `typography` | title 20/600, subtitle 16/600, body 14/400, caption 12/400, label 11/500 |
| `elevation` | card 1, fab 3, dialog 6 |

`paperTheme.ts`: map tokens vào `MD3LightTheme`/`MD3DarkTheme` (colors, roundness, fonts) → toàn app dùng 1 theme.

### 4.2 Component inventory (props cố định, dùng lại mọi màn hình)

| Component | Props chính | Ghi chú |
|---|---|---|
| `AppButton` | `label, onPress, variant(primary/outline/danger/text), loading, disabled, icon` | loading → spinner trong nút |
| `FormField` | `label, value, onChangeText, error, helperText, secure, keyboardType, maxLength, right` | dùng `TextInput` outlined + `HelperText` |
| `StateView` | `status: loading/empty/error, onRetry?, emptyText?, errorText?` | mọi màn hình dữ liệu dùng chung |
| `ProjectCard` | `project, onPress` | tên, mô tả 2 dòng, `RoleChip`, ngày tạo |
| `RoleChip` | `role` | màu + label theo vai trò |
| `MemberRow` | `member` | avatar chữ cái đầu, tên, email, chip vai trò |
| `InviteCard` | `code, canManage, onCreate, onCopy, onRevoke` | chỉ render với OWNER/ADMIN |
| `SkeletonList` | `count` | shimmer nhẹ bằng Reanimated khi tải |
| `SnackbarProvider` | `show(message, type)` | hàng đợi snackbar toàn app |

### 4.3 Chuẩn trạng thái mỗi màn hình

Mọi màn hình có dữ liệu khai báo đủ: **loading** (`SkeletonList`/`ActivityIndicator`), **empty**
(icon + text + nút hành động), **error** (text + "Thử lại"), **content**. Form: lỗi hiện dưới field
(`HelperText type="error"`), lỗi hệ thống → Snackbar.

---

## 5. Khai báo chi tiết từng màn hình

### 5.1 `app/index.tsx` — Splash/Redirect
- `SafeAreaView` + logo + `ActivityIndicator` + text `Đang kiểm tra phiên…`.
- Logic: `SessionProvider` đọc token; có token → thử `useMe`/refresh → `/(app)/projects`; lỗi → `/(auth)/login`.
- Không input. Nếu chờ > 3s hiện banner `Máy chủ đang khởi động…` (fade-in, Reanimated).

### 5.2 `(auth)/login.tsx`
- Layout: `KeyboardAvoidingView` → `ScrollView` → logo, title, `FormField` email, `FormField` password
  (`secure`, icon hiện/ẩn), `AppButton "Đăng nhập"`, `TextButton "Quên mật khẩu"`, `TextButton "Đăng ký"`.
- Zod: `email: z.string().email()`, `password: z.string().min(1)`.
- API `POST /auth/login` → lưu token (SecureStore) + user vào session → replace `/(app)/projects`.
- Lỗi: `AUTH_INVALID_CREDENTIALS` → lỗi dưới password; `AUTH_RATE_LIMITED` → Snackbar; lỗi mạng/timeout →
  Snackbar + nút thử lại. Nút disable khi submitting.

### 5.3 `(auth)/register.tsx`
- Fields: fullName, email, password, confirmPassword. HelperText "Mật khẩu tối thiểu 8 ký tự".
- Zod: `fullName 1–100`, `email`, `password min 8 max 72`, `confirm === password`.
- API `POST /auth/register` → auto login → `/(app)/projects`. `USER_EMAIL_EXISTS` → lỗi dưới email.

### 5.4 `(auth)/forgot-password.tsx`
- Field email + nút gửi + link quay lại Login.
- API `POST /auth/forgot-password` → luôn 204 → Snackbar `Nếu email tồn tại, chúng tôi đã gửi liên kết đặt lại mật khẩu` → về Login.

### 5.5 `(app)/projects/index.tsx` — Danh sách dự án
- Header: title `Dự án`, action phải: icon profile (→ `/(app)/profile`); header trái: không back.
- Danh sách `FlatList` + `RefreshControl` (kéo làm mới) → `ProjectCard` → `/(app)/projects/[id]`.
- FAB `Tạo dự án` (→ `/(app)/projects/new`) + `AppButton` "Tham gia bằng mã" cuối danh sách (hoặc menu header).
- States: `SkeletonList` khi `isLoading`; empty state (icon + "Chưa có dự án nào" + nút tạo); error (retry).
- API `GET /projects` (TanStack Query, `refetchOnFocus`, staleTime 30s).

### 5.6 `(app)/projects/new.tsx`
- Fields name (maxLength 100, counter), description (multiline, 1000). Nút `Tạo`.
- Zod: `name 1–100`, `description ≤ 1000` optional.
- API `POST /projects` → 201 (`role=OWNER`) → Snackbar + `router.replace('/(app)/projects/[id]')`.

### 5.7 `(app)/projects/join.tsx`
- Field code: auto uppercase, filter `[A-Z0-9]`, maxLength 12; helper "Mã mời gồm 12 ký tự".
- API `POST /projects/join` → 200 → `router.replace('/(app)/projects/[id]')`; `INVITE_INVALID` → lỗi dưới code.

### 5.8 `(app)/projects/[id].tsx` — Chi tiết dự án
- Header title = tên dự án; body scroll:
  - Mô tả + `RoleChip` (vai trò của mình).
  - `InviteCard` (chỉ OWNER/ADMIN): mã hiện có / nút tạo, copy (expo-clipboard), thu hồi.
  - Tiêu đề `Thành viên (n)` + danh sách `MemberRow`.
- API: `GET /projects/{id}`, `GET /projects/{id}/members`; OWNER/ADMIN thêm `GET /projects/{id}/invites`.
- Mutations: tạo/thu hồi invite (invalidate query); copy → Snackbar.
- Lỗi 403 (`PROJECT_FORBIDDEN`) → Snackbar + back. Không có hành động ghi với VIEWER.

### 5.9 `(app)/profile.tsx`
- Avatar chữ cái đầu + fullName (editable) + email (read-only) + nút `Lưu`.
- Section `Đổi mật khẩu`: old/new password + nút; helper ≥ 8 ký tự.
- Nút `Đăng xuất` (danger, dialog xác nhận).
- API: `GET /users/me`, `PATCH /users/me`, `POST /users/me/password` (thành công → logout → `/(auth)/login`),
  `POST /auth/logout` (gửi refreshToken) → xoá SecureStore + session → replace Login.

### 5.10 Luồng route

```
/ (index: splash)
 ├─(có phiên)→ /(app)/projects ──→ /(app)/projects/[id]
 │                 ├─ FAB → /(app)/projects/new
 │                 ├─ button → /(app)/projects/join → [id]
 │                 └─ header icon → /(app)/profile
 └─(không phiên)→ /(auth)/login ──→ /(auth)/register
                       └─→ /(auth)/forgot-password → (về login)
```
- `(auth)` guard: request 401 hết hạn refresh → `router.replace('/(auth)/login')`; `(app)` guard:
  không có token → về login (dùng `useProtectedRoute`).

---

## 6. Tầng mạng & phiên

- `src/api/client.ts`: axios instance `baseURL = process.env.EXPO_PUBLIC_API_URL`, `timeout: 60000`,
  header JSON; request interceptor gắn `Authorization: Bearer <accessToken>` (bỏ qua `/auth/**`).
- **Refresh single-flight**: response interceptor bắt 401 (trừ request refresh) → dùng 1 promise chung
  gọi `POST /auth/refresh` → lưu token mới → retry request gốc; refresh fail → `session.clear()` +
  emit `sessionExpired` → router về Login.
- `src/api/errors.ts`: parse `{code,message,path}` → `ApiError{code,message,status}`; map timeout/network
  → message thân thiện; hàm `fieldErrorFromApi(error)` cho form.
- TanStack Query: `retry: 2` với exponential backoff (cold start Render), `staleTime` 30s,
  `refetchOnWindowFocus` (RN: AppState resume).
- Cold-start UX: nếu request chạy > 3s → hiện banner `Máy chủ đang khởi động, vui lòng đợi…`
  (fade + pulse bằng Reanimated); lỗi timeout → StateView error + "Thử lại".
- `SessionProvider` (Context): `{ user, accessToken, refreshToken, signIn, signOut, clear, isReady }`;
  token lưu bằng `expo-secure-store` (key: `access_token`, `refresh_token`, `user`).

---

## 7. Types & schemas (hợp đồng với backend)

- `src/types/api.ts`: `UserDto{id,email,fullName,avatarUrl}`, `TokenResponse{accessToken,refreshToken,
  accessTokenExpiresAt,user}`, `ProjectDto{id,name,description,ownerId,role,createdAt}`,
  `MemberDto{userId,fullName,email,role,joinedAt}`, `InviteDto{id,code,inviteUrl,expiresAt}`,
  `ApiError{code,message,status}`.
- `src/schemas/auth.ts`: `loginSchema`, `registerSchema`, `forgotPasswordSchema`, `changePasswordSchema`.
- `src/schemas/project.ts`: `projectSchema`, `joinProjectSchema`.
- Validation **phải khớp backend**: email hợp lệ; password 8–72; fullName ≤ 100; project name 1–100;
  description ≤ 1000; invite code 12 ký tự `[A-Z0-9]`.

---

## 8. Kiểm thử & CI

- **Unit test (Jest)**: zod schemas (biên 7/8/72 ký tự), `errors.ts` (map code), `client.ts` refresh
  single-flight (mock adapter: 401 → refresh → retry; refresh fail → sessionExpired).
- **Component test (@testing-library/react-native)**: Login form (submit thiếu email → hiện lỗi; submit
  hợp lệ → gọi repository), StateView (empty/error/retry), RoleChip.
- **QA thủ công**: đăng ký → đăng nhập → danh sách → tạo → link mời → tài khoản B join → đổi vai trò;
  tắt mạng → StateView lỗi + retry; để Render ngủ 15' → banner "server đang khởi động" rồi vào được.
- **CI** `.github/workflows/mobile-ci.yml` (paths `mobile/**`): `npm ci` → `npx tsc --noEmit` →
  `npm test -- --ci` → `npx expo-doctor`. Không build APK trong CI (EAS build riêng khi cần).

---

## 9. Tích hợp repo & tài liệu

- App đặt tại `mobile/`; **xoá `android/`** (project Java scaffold vừa tạo — không dùng nữa; cần bạn
  xác nhận trước khi xoá).
- `app.config.ts`: `name: "RemoteApp"`, `slug: "remoteapp"`, `scheme: "remoteapp"`,
  `android.package: "com.example.remoteapp"`, `android.versionCode`, icon/splash.
- `.env.example` + README hướng dẫn `EXPO_PUBLIC_API_URL` (Render URL; dev local dùng IP LAN nếu chạy
  backend trên máy — Expo Go trên điện thoại thật không dùng được `localhost`).
- README: mục Android/Mobile cập nhật (Expo RN + TS), nêu **lý do chuyển từ native Java/Kotlin sang RN**
  (quen React, UI/animation, tránh Kotlin) để phục vụ báo cáo.
- AGENTS.md: cập nhật ràng buộc mobile + command (`npm ci`, `npx expo start`, `tsc`, `jest`) + quy ước
  (TS strict, zod, TanStack Query, Paper theme, không hardcode màu/spacing).

---

## 10. Definition of Done (phase 1 app)

- [ ] `npx tsc --noEmit` + `npm test` xanh; `expo-doctor` sạch.
- [ ] Chạy trên máy thật/emulator: đăng ký → đăng nhập → auto-login (mở lại app vẫn vào) → đăng xuất.
- [ ] Refresh token tự động khi access token hết hạn (test TTL ngắn khi dev).
- [ ] Danh sách dự án: skeleton → content; empty state; kéo làm mới; lỗi mạng → retry.
- [ ] Tạo dự án, join bằng mã (sai mã → lỗi đúng trường), chi tiết + thành viên.
- [ ] Link mời: OWNER/ADMIN tạo/copy/thu hồi; MEMBER/VIEWER không thấy nút.
- [ ] Hồ sơ: sửa tên, đổi mật khẩu (thu hồi phiên → về Login), đăng xuất.
- [ ] Cold start Render: banner "server đang khởi động" + retry hoạt động.
- [ ] Mọi text tiếng Việt trong `constants`/i18n file; dark mode (Paper theme) đúng; không hardcode style.
- [ ] Không commit `.env`, `node_modules`, `.expo`.

---

## 11. Thứ tự thực hiện

1. `npx create-expo-app mobile --template` (TypeScript + Expo Router) → cấu hình `app.config.ts`,
   `.env.example`, thêm dependency (Paper, TanStack Query, axios, zod, RHF, SecureStore...).
2. Theme tokens + `paperTheme` + component inventory (AppButton, FormField, StateView, RoleChip...).
3. Core: `api/client.ts` (interceptors + refresh), `api/errors.ts`, types, schemas, session context,
   query client, route guards.
4. Auth flow: index (splash), login, register, forgot → chạy thật với backend Render.
5. Dự án: list (skeleton/empty/error + pull-to-refresh), new, join, detail (members + invite theo role).
6. Profile: xem/sửa tên, đổi mật khẩu, logout.
7. Unit/component test + CI mobile.
8. Xoá `android/` (sau khi bạn xác nhận), cập nhật README/AGENTS, commit + push.

---

## 12. Rủi ro

| Rủi ro | Giảm thiểu |
|---|---|
| Render free cold start 30–60s | timeout 60s, TanStack Query retry backoff, banner "server đang khởi động" |
| Refresh token đồng thời | single-flight promise trong axios interceptor |
| Expo Go trên máy thật không gọi được `localhost` | dùng URL Render (HTTPS) hoặc IP LAN của máy dev |
| EAS Build free phải xếp hàng | build APK trước ngày demo; dự phòng `npx expo run:android` |
| UI không đồng nhất | chỉ dùng theme tokens + component inventory, không style rời rạc |
| Trái "Android native" trong SRS | ghi rõ quyết định trong README/báo cáo: RN vẫn build ra APK Android, lý do chuyển đổi |
