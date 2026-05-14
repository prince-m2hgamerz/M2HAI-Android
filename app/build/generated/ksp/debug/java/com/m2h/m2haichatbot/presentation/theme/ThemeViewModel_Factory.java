package com.m2h.m2haichatbot.presentation.theme;

import com.m2h.m2haichatbot.data.repository.PreferenceRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class ThemeViewModel_Factory implements Factory<ThemeViewModel> {
  private final Provider<PreferenceRepository> preferenceRepositoryProvider;

  public ThemeViewModel_Factory(Provider<PreferenceRepository> preferenceRepositoryProvider) {
    this.preferenceRepositoryProvider = preferenceRepositoryProvider;
  }

  @Override
  public ThemeViewModel get() {
    return newInstance(preferenceRepositoryProvider.get());
  }

  public static ThemeViewModel_Factory create(
      Provider<PreferenceRepository> preferenceRepositoryProvider) {
    return new ThemeViewModel_Factory(preferenceRepositoryProvider);
  }

  public static ThemeViewModel newInstance(PreferenceRepository preferenceRepository) {
    return new ThemeViewModel(preferenceRepository);
  }
}
