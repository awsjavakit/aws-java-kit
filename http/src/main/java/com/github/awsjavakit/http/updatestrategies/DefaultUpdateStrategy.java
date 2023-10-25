package com.github.awsjavakit.http.updatestrategies;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import java.util.function.Supplier;

public class DefaultUpdateStrategy implements UpdateCollisionStrategy{

  @Override
  public void avoidCollision() {

  }

  @Override
  public void updateToken(Supplier<OAuthTokenEntry> fetchCachedToken,
    Supplier<OAuthTokenEntry> updateToken) {


  }
}
