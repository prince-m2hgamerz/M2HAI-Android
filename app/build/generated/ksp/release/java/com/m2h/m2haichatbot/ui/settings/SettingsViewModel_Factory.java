package com.m2h.m2haichatbot.ui.settings;

import com.m2h.m2haichatbot.data.repository.PreferenceRepository;
import com.m2h.m2haichatbot.domain.repository.AuthRepository;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<PreferenceRepository> preferenceRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public SettingsViewModel_Factory(Provider<PreferenceRepository> preferenceRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.preferenceRepositoryProvider = preferenceRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(preferenceRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<PreferenceRepository> preferenceRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new SettingsViewModel_Factory(preferenceRepositoryProvider, authRepositoryProvider);
  }

  public static SettingsViewModel newInstance(PreferenceRepository preferenceRepository,
      AuthRepository authRepository) {
    return new SettingsViewModel(preferenceRepository, authRepository);
  }
}
