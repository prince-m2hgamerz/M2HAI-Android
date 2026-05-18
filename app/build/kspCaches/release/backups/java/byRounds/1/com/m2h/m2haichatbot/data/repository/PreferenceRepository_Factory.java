package com.m2h.m2haichatbot.data.repository;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class PreferenceRepository_Factory implements Factory<PreferenceRepository> {
  private final Provider<Context> contextProvider;

  public PreferenceRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public PreferenceRepository get() {
    return newInstance(contextProvider.get());
  }

  public static PreferenceRepository_Factory create(Provider<Context> contextProvider) {
    return new PreferenceRepository_Factory(contextProvider);
  }

  public static PreferenceRepository newInstance(Context context) {
    return new PreferenceRepository(context);
  }
}
