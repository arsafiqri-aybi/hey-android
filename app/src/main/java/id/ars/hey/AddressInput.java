package id.ars.hey;

import java.net.URLEncoder;

final class AddressInput {
  static String resolve(String input) throws Exception {
    String value = input.trim();
    if (value.isEmpty()) throw new IllegalArgumentException("EMPTY_ADDRESS");
    if (value.contains("://") || value.matches("(?i)^(javascript|data|file|about|intent):.*")) {
      UrlPolicy.validate(value);
      return value;
    }
    if (!value.contains(" ") && value.matches("(?i)^[a-z0-9][a-z0-9.-]+\\.[a-z]{2,}([/?#].*)?$")) {
      String url = "https://" + value;
      UrlPolicy.validate(url);
      return url;
    }
    return "https://www.google.com/search?q=" + URLEncoder.encode(value, "UTF-8");
  }
}
