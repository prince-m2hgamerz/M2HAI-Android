package com.m2h.m2haichatbot.presentation.update;

import com.m2h.m2haichatbot.domain.repository.ModelRepository;
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
public final class AppUpdateViewModel_Factory implements Factory<AppUpdateViewModel> {
  private final Provider<ModelRepository> modelRepositoryProvider;

  public AppUpdateViewModel_Factory(Provider<ModelRepository> modelRepositoryProvider) {
    this.modelRepositoryProvider = modelRepositoryProvider;
  }

  @Override
  public AppUpdateViewModel get() {
    return newInstance(modelRepositoryProvider.get());
  }

  public static AppUpdateViewModel_Factory create(
      Provider<ModelRepository> modelRepositoryProvider) {
    return new AppUpdateViewModel_Factory(modelRepositoryProvider);
  }

  public static AppUpdateViewModel newInstance(ModelRepository modelRepository) {
    return new AppUpdateViewModel(modelRepository);
  }
}
