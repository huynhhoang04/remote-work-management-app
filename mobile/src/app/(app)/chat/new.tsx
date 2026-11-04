import { router, Stack } from 'expo-router';
import React, { useEffect, useMemo, useState } from 'react';
import { FlatList, StyleSheet, View } from 'react-native';
import { Avatar, Chip, Text, TouchableRipple, useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { AppButton } from '@/components/AppButton';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { useCreateDirectConversation } from '@/features/chat/hooks';
import { useMembers, useProjects } from '@/features/projects/hooks';
import { useSnackbar } from '@/components/SnackbarProvider';
import { useSession } from '@/store/session';
import { radius, spacing } from '@/theme/tokens';
import type { MemberDto } from '@/types/api';
import { colorFromString, initials } from '@/utils/format';
import { STR } from '@/utils/constants';

/**
 * Bắt đầu chat 1-1: chọn dự án → chọn thành viên.
 * @reference docs/plan-app/02-phase-2.md §5.8
 */
export default function NewChatScreen() {
  const theme = useTheme();
  const { show } = useSnackbar();
  const { user } = useSession();
  const projects = useProjects();
  const [projectId, setProjectId] = useState<string | null>(null);
  const members = useMembers(projectId ?? '');
  const createDirect = useCreateDirectConversation();

  useEffect(() => {
    if (!projectId && projects.data && projects.data.length > 0) {
      setProjectId(projects.data[0].id);
    }
  }, [projectId, projects.data]);

  const candidates = useMemo(
    () => (members.data ?? []).filter((member) => member.userId !== user?.id),
    [members.data, user?.id],
  );

  const openChat = async (member: MemberDto) => {
    try {
      const conversation = await createDirect.mutateAsync(member.userId);
      router.replace(`/chat/${conversation.id}`);
    } catch (error) {
      show(normalizeError(error).message);
    }
  };

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen options={{ title: STR.chat.newDirect }} />
      {projects.isPending ? (
        <SkeletonList count={3} />
      ) : projects.isError ? (
        <StateView status="error" errorText={normalizeError(projects.error).message} onRetry={() => projects.refetch()} />
      ) : (projects.data?.length ?? 0) === 0 ? (
        <StateView status="empty" icon="folder-outline" emptyText={STR.projects.empty} emptyHint={STR.projects.emptyHint} />
      ) : (
        <View style={styles.body}>
          <Text variant="labelLarge" style={[styles.label, { color: theme.colors.onSurfaceVariant }]}>
            {STR.chat.selectProject}
          </Text>
          <View style={styles.chips}>
            {projects.data?.map((project) => (
              <Chip
                key={project.id}
                selected={project.id === projectId}
                onPress={() => setProjectId(project.id)}
                style={styles.chip}
              >
                {project.name}
              </Chip>
            ))}
          </View>

          <Text variant="labelLarge" style={[styles.label, { color: theme.colors.onSurfaceVariant }]}>
            {STR.chat.selectMember}
          </Text>
          {members.isPending ? (
            <SkeletonList count={3} />
          ) : candidates.length === 0 ? (
            <StateView status="empty" icon="account-off-outline" emptyText={STR.projects.noMembers} />
          ) : (
            <FlatList
              data={candidates}
              keyExtractor={(item) => item.userId}
              renderItem={({ item }) => {
                const color = colorFromString(item.userId);
                return (
                  <TouchableRipple onPress={() => void openChat(item)}>
                  <View style={[styles.memberRow, { borderBottomColor: theme.colors.outline }]}>
                    <Avatar.Text
                      size={42}
                      label={initials(item.fullName)}
                      style={{ backgroundColor: `${color}1F` }}
                      labelStyle={{ color, fontWeight: '700', fontSize: 14 }}
                    />
                    <View style={styles.flex}>
                      <Text variant="titleSmall" numberOfLines={1}>
                        {item.fullName}
                      </Text>
                      <Text variant="bodySmall" numberOfLines={1} style={{ color: theme.colors.onSurfaceVariant }}>
                        {item.email}
                      </Text>
                    </View>
                    <AppButton
                      label={STR.chat.direct}
                      variant="text"
                      onPress={() => void openChat(item)}
                      loading={createDirect.isPending}
                    />
                  </View>
                  </TouchableRipple>
                );
              }}
            />
          )}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  body: { flex: 1, padding: spacing.lg },
  label: { fontWeight: '600', marginBottom: spacing.xs },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.xs, marginBottom: spacing.lg },
  chip: { borderRadius: 999 },
  memberRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    paddingVertical: spacing.sm,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  flex: { flex: 1 },
  radius: { borderRadius: radius.md },
});
