package com.m2h.m2haichatbot.ui.auth;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.gotrue.Auth;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class AuthViewModel_Factory implements Factory<AuthViewModel> {
  private final Provider<Auth> authProvider;

  public AuthViewModel_Factory(Provider<Auth> authProvider) {
    this.authProvider = authProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(authProvider.get());
  }

  public static AuthViewModel_Factory create(Provider<Auth> authProvider) {
    return new AuthViewModel_Factory(authProvider);
  }

  public static AuthViewModel newInstance(Auth auth) {
    return new AuthViewModel(auth);
  }
}
