import { router } from 'expo-router';
import React, { useCallback, useMemo, useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { ActivityIndicator, useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { AppButton } from '@/components/AppButton';
import { ConversationRow } from '@/components/ConversationRow';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { TabHeader } from '@/components/TabHeader';
import { useConversations } from '@/features/chat/hooks';
import { spacing } from '@/theme/tokens';
import type { ConversationDto } from '@/types/api';
import { STR } from '@/utils/constants';

/**
 * Tab Chat: danh sách hội thoại GROUP (theo dự án) + DIRECT.
 * @reference docs/plan-app/02-phase-2.md §5.8
 */
export default function ChatListScreen() {
  const theme = useTheme();
  const conversations = useConversations();
  const [refreshing, setRefreshing] = useState(false);

  const sorted = useMemo(() => {
    const items = conversations.data ?? [];
    return [...items].sort((a, b) => {
      const aTime = new Date(a.lastMessage?.createdAt ?? a.createdAt).getTime();
      const bTime = new Date(b.lastMessage?.createdAt ?? b.createdAt).getTime();
      return bTime - aTime;
    });
  }, [conversations.data]);

  const openConversation = useCallback((conversation: ConversationDto) => {
    router.push(`/chat/${conversation.id}`);
  }, []);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await conversations.refetch();
    } finally {
      setRefreshing(false);
    }
  }, [conversations]);

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <TabHeader
        title={STR.chat.title}
        actions={[{ icon: 'plus', onPress: () => router.push('/chat/new'), accessibilityLabel: STR.chat.newDirect }]}
      />

      {conversations.isPending ? (
        <View style={styles.skeleton}>
          <SkeletonList count={5} />
        </View>
      ) : conversations.isError ? (
        <StateView
          status="error"
          errorText={normalizeError(conversations.error).message}
          onRetry={() => conversations.refetch()}
        />
      ) : sorted.length === 0 ? (
        <StateView
          status="empty"
          icon="message-text-outline"
          emptyText={STR.chat.empty}
          emptyHint={STR.chat.emptyHint}
        >
          <AppButton label={STR.chat.newDirect} icon="plus" onPress={() => router.push('/chat/new')} />
        </StateView>
      ) : (
        <FlatList
          data={sorted}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => <ConversationRow conversation={item} onPress={openConversation} />}
          contentContainerStyle={styles.list}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={theme.colors.primary} />
          }
          ListFooterComponent={
            conversations.isRefetching && !refreshing ? <ActivityIndicator style={styles.footer} /> : undefined
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  skeleton: { paddingTop: spacing.sm },
  list: { paddingBottom: spacing.xxl },
  footer: { paddingVertical: spacing.lg },
});
