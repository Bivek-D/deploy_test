@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final Map<String, String> refreshTokens = new ConcurrentHashMap<>();

    @Value("${app.cookie-secure:false}")
    private boolean cookieSecure;

    @GetMapping("/ping")
    public Map<String, Object> ping(HttpServletRequest req) {
        return Map.of("status", "ok", "time", Instant.now().toString(),
                "remoteAddr", req.getRemoteAddr(),
                "xForwardedFor", String.valueOf(req.getHeader("X-Forwarded-For")));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login() {
        return issue("tester");
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @CookieValue(value = "refresh", required = false) String refresh) {
        String user = refresh == null ? null : refreshTokens.remove(refresh);
        if (user == null) return ResponseEntity.status(401).body(Map.of("message", "No valid refresh cookie"));
        return issue(user);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        if (!valid(auth)) return ResponseEntity.status(401).body(Map.of("message", "Missing or expired token"));
        return ResponseEntity.ok(Map.of("message", "Access token accepted"));
    }

    private ResponseEntity<Map<String, Object>> issue(String user) {
        String refresh = UUID.randomUUID().toString();
        refreshTokens.put(refresh, user);
        ResponseCookie cookie = ResponseCookie.from("refresh", refresh)
                .httpOnly(true).secure(cookieSecure).sameSite("Lax")
                .path("/api/v1/auth").maxAge(Duration.ofDays(7)).build();
        long exp = Instant.now().getEpochSecond() + 30;   // 30 seconds, to test refresh fast
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("tester:" + exp).getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("accessToken", token));
    }

    private boolean valid(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return false;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(auth.substring(7)), StandardCharsets.UTF_8);
            return Long.parseLong(raw.substring(raw.lastIndexOf(':') + 1)) > Instant.now().getEpochSecond();
        } catch (Exception e) { return false; }
    }
}