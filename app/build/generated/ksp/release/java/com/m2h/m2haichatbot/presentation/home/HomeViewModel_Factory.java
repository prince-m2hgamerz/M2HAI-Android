package com.m2h.m2haichatbot.presentation.home;

import com.m2h.m2haichatbot.domain.repository.AuthRepository;
import com.m2h.m2haichatbot.domain.repository.ChatRepository;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<ModelRepository> modelRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public HomeViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<ModelRepository> modelRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.modelRepositoryProvider = modelRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(chatRepositoryProvider.get(), modelRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<ChatRepository> chatRepositoryProvider,
      Provider<ModelRepository> modelRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new HomeViewModel_Factory(chatRepositoryProvider, modelRepositoryProvider, authRepositoryProvider);
  }

  public static HomeViewModel newInstance(ChatRepository chatRepository,
      ModelRepository modelRepository, AuthRepository authRepository) {
    return new HomeViewModel(chatRepository, modelRepository, authRepository);
  }
}
