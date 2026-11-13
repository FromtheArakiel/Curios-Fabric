package dev.arakiel.curiosfabric.server.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import dev.arakiel.curiosfabric.api.CuriosApi;

public class CurioArgumentType implements ArgumentType<String> {

  public static Set<String> slotIds = new HashSet<>();

  private static final Collection<String> EXAMPLES = Arrays.asList("ring", "head");
  private static final DynamicCommandExceptionType UNKNOWN_TYPE = new DynamicCommandExceptionType(
      type -> Component.translatable("argument.curiosfabric.type.unknown", type));

  public static CurioArgumentType slot() {
    return new CurioArgumentType();
  }

  public static String getSlot(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, String.class);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
                                                            SuggestionsBuilder builder) {
    return SharedSuggestionProvider.suggest(slotIds, builder);
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }

  @Override
  public String parse(StringReader reader) throws CommandSyntaxException {
    String s = reader.readUnquotedString();

    if (CuriosApi.getSlotHelper() != null && !slotIds.contains(s)) {
      throw UNKNOWN_TYPE.create(s);
    } else {
      return s;
    }
  }
}