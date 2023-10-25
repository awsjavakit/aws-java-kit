package com.github.awsjavakit.http.updatestrategies;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import java.util.function.Supplier;

public interface UpdateCollisionStrategy {

  void avoidCollision();

  void updateToken(Supplier<OAuthTokenEntry> fetchCachedToken, Supplier<OAuthTokenEntry> updateToken);
}
