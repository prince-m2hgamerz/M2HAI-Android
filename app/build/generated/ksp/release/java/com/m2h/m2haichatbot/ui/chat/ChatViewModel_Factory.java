package com.m2h.m2haichatbot.ui.chat;

import android.content.Context;
import com.m2h.m2haichatbot.domain.repository.ChatRepository;
import com.m2h.m2haichatbot.domain.repository.ModelRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<ChatRepository> repositoryProvider;

  private final Provider<ModelRepository> modelRepositoryProvider;

  private final Provider<Context> contextProvider;

  public ChatViewModel_Factory(Provider<ChatRepository> repositoryProvider,
      Provider<ModelRepository> modelRepositoryProvider, Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.modelRepositoryProvider = modelRepositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(repositoryProvider.get(), modelRepositoryProvider.get(), contextProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<ChatRepository> repositoryProvider,
      Provider<ModelRepository> modelRepositoryProvider, Provider<Context> contextProvider) {
    return new ChatViewModel_Factory(repositoryProvider, modelRepositoryProvider, contextProvider);
  }

  public static ChatViewModel newInstance(ChatRepository repository,
      ModelRepository modelRepository, Context context) {
    return new ChatViewModel(repository, modelRepository, context);
  }
}
