import * as Clipboard from 'expo-clipboard';
import { router, Stack, useLocalSearchParams } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Card, Divider, Icon, Text, TouchableRipple, useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { InviteCard } from '@/components/InviteCard';
import { MemberRow } from '@/components/MemberRow';
import { RoleChip } from '@/components/RoleChip';
import { useSnackbar } from '@/components/SnackbarProvider';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import {
  useCreateInvite,
  useInvites,
  useMembers,
  useProject,
  useRevokeInvite,
} from '@/features/projects/hooks';
import { radius, spacing } from '@/theme/tokens';
import { formatDate } from '@/utils/format';
import { STR } from '@/utils/constants';

interface Shortcut {
  icon: string;
  label: string;
  onPress: () => void;
  color: string;
}

/**
 * Tổng quan dự án + lối tắt tới công việc/thành viên/thống kê/cài đặt.
 * @reference docs/plan-app/02-phase-2.md §5.3
 */
export default function ProjectDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const { show } = useSnackbar();

  const project = useProject(id);
  const members = useMembers(id);
  const canManage = project.data?.role === 'OWNER' || project.data?.role === 'ADMIN';
  const isOwner = project.data?.role === 'OWNER';
  const invites = useInvites(id, Boolean(canManage));
  const createInvite = useCreateInvite(id);
  const revokeInvite = useRevokeInvite(id);

  const invite = invites.data?.[0] ?? null;
  const isLoading = project.isPending || members.isPending;
  const error = project.error ?? members.error;

  const onCreateInvite = async () => {
    try {
      await createInvite.mutateAsync(undefined);
      show(STR.projects.inviteCreated);
    } catch (inviteError) {
      show(normalizeError(inviteError).message);
    }
  };

  const onCopy = async (code: string) => {
    await Clipboard.setStringAsync(code);
    show(STR.projects.copied);
  };

  const onRevoke = async (inviteId: string) => {
    try {
      await revokeInvite.mutateAsync(inviteId);
      show(STR.projects.inviteRevoked);
    } catch (inviteError) {
      show(normalizeError(inviteError).message);
    }
  };

  if (isLoading) {
    return (
      <View style={[styles.fill, { backgroundColor: theme.colors.background }]}>
        <Stack.Screen options={{ title: '' }} />
        <SkeletonList count={3} />
      </View>
    );
  }

  if (error || !project.data) {
    return (
      <StateView
        status="error"
        errorText={normalizeError(error).message}
        onRetry={() => {
          void project.refetch();
          void members.refetch();
        }}
      />
    );
  }

  const shortcuts: Shortcut[] = [
    { icon: 'view-column-outline', label: STR.projectsExtra.tasksShortcut, color: '#2C63E8', onPress: () => router.push(`/projects/${id}/kanban`) },
    { icon: 'account-group-outline', label: STR.projectsExtra.membersShortcut, color: '#7C3AED', onPress: () => router.push(`/projects/${id}/members`) },
    { icon: 'chart-donut', label: STR.projectsExtra.statsShortcut, color: '#16A34A', onPress: () => router.push(`/projects/${id}/stats`) },
    { icon: 'label-outline', label: STR.projectsExtra.labelsShortcut, color: '#F97316', onPress: () => router.push(`/projects/${id}/labels`) },
  ];
  if (canManage) {
    shortcuts.push({ icon: 'cog-outline', label: STR.projectsExtra.settings, color: '#64748B', onPress: () => router.push(`/projects/${id}/settings`) });
  }
  if (isOwner) {
    shortcuts.push({ icon: 'history', label: STR.projectsExtra.audit, color: '#0EA5E9', onPress: () => router.push(`/projects/${id}/audit`) });
  }

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen options={{ title: project.data.name }} />
      <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
        <Card mode="elevated" style={styles.card}>
          <Card.Content>
            <View style={styles.titleRow}>
              <Text variant="titleLarge" style={styles.name}>
                {project.data.name}
              </Text>
              <RoleChip role={project.data.role} />
            </View>
            {project.data.description ? (
              <Text variant="bodyMedium" style={[styles.description, { color: theme.colors.onSurfaceVariant }]}>
                {project.data.description}
              </Text>
            ) : null}
            <View style={styles.metaRow}>
              <Icon source="calendar-blank-outline" size={14} color={theme.colors.onSurfaceVariant} />
              <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
                {STR.projects.createdOn} {formatDate(project.data.createdAt)}
              </Text>
            </View>
          </Card.Content>
        </Card>

        <View style={styles.shortcuts}>
          {shortcuts.map((shortcut) => (
            <TouchableRipple
              key={shortcut.label}
              onPress={shortcut.onPress}
              borderless
              style={styles.shortcutWrap}
            >
              <View style={[styles.shortcut, { backgroundColor: theme.colors.surface, borderColor: theme.colors.outline }]}>
                <View style={[styles.shortcutIcon, { backgroundColor: `${shortcut.color}19` }]}>
                  <Icon source={shortcut.icon} size={22} color={shortcut.color} />
                </View>
                <Text variant="labelLarge" numberOfLines={2} style={styles.shortcutLabel}>
                  {shortcut.label}
                </Text>
                <Icon source="chevron-right" size={18} color={theme.colors.onSurfaceVariant} />
              </View>
            </TouchableRipple>
          ))}
        </View>

        <InviteCard
          invite={invite}
          canManage={Boolean(canManage)}
          creating={createInvite.isPending}
          onCreate={onCreateInvite}
          onCopy={onCopy}
          onRevoke={onRevoke}
        />

        <View style={styles.sectionHeader}>
          <Icon source="account-group-outline" size={18} color={theme.colors.primary} />
          <Text variant="titleSmall" style={styles.sectionTitle}>
            {STR.projects.membersTitle} ({members.data?.length ?? 0})
          </Text>
          <Text
            variant="labelMedium"
            style={{ color: theme.colors.primary }}
            onPress={() => router.push(`/projects/${id}/members`)}
          >
            Xem tất cả
          </Text>
        </View>

        <Card mode="elevated" style={styles.card}>
          <Card.Content style={styles.noPadding}>
            {members.data && members.data.length > 0 ? (
              members.data.slice(0, 5).map((member, index) => (
                <View key={member.userId}>
                  {index > 0 ? <Divider /> : null}
                  <MemberRow member={member} />
                </View>
              ))
            ) : (
              <Text variant="bodyMedium" style={[styles.emptyMembers, { color: theme.colors.onSurfaceVariant }]}>
                {STR.projects.noMembers}
              </Text>
            )}
          </Card.Content>
        </Card>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  fill: { flex: 1, paddingTop: spacing.lg },
  container: { padding: spacing.lg, gap: spacing.md, paddingBottom: spacing.xxl },
  card: { borderRadius: radius.lg },
  titleRow: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.sm },
  name: { flex: 1, fontWeight: '700', lineHeight: 28 },
  description: { marginTop: spacing.sm },
  metaRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs, marginTop: spacing.md },
  shortcuts: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  shortcutWrap: { flexBasis: '47%', flexGrow: 1, borderRadius: radius.lg },
  shortcut: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    padding: spacing.md,
    borderRadius: radius.lg,
    borderWidth: StyleSheet.hairlineWidth,
    minHeight: 64,
  },
  shortcutIcon: { width: 38, height: 38, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center' },
  shortcutLabel: { flex: 1, fontWeight: '600' },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginTop: spacing.sm },
  sectionTitle: { flex: 1, fontWeight: '600' },
  noPadding: { padding: 0 },
  emptyMembers: { padding: spacing.lg },
});
