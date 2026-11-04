import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Button, Card, Icon, Text, useTheme } from 'react-native-paper';
import { radius, spacing } from '../theme/tokens';
import type { InviteDto } from '../types/api';
import { STR } from '../utils/constants';

interface Props {
  invite?: InviteDto | null;
  canManage: boolean;
  creating?: boolean;
  onCreate: () => void;
  onCopy: (code: string) => void;
  onRevoke: (inviteId: string) => void;
}

/**
 * Card link mời dự án — chỉ hiển thị với OWNER/ADMIN (UI phản ánh RBAC).
 * @reference docs/plan-app/01-phase-1.md §5.8
 */
export function InviteCard({ invite, canManage, creating = false, onCreate, onCopy, onRevoke }: Props) {
  const theme = useTheme();

  if (!canManage) return null;

  return (
    <Card mode="elevated" style={styles.card}>
      <Card.Content>
        <View style={styles.header}>
          <Icon source="link-variant" size={20} color={theme.colors.primary} />
          <Text variant="titleSmall" style={styles.title}>
            {STR.projects.inviteTitle}
          </Text>
        </View>

        {invite ? (
          <>
            <View style={[styles.codeBox, { backgroundColor: theme.colors.surfaceVariant }]}>
              <Text variant="titleLarge" style={styles.code}>
                {invite.code}
              </Text>
            </View>
            <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
              {STR.projects.inviteHint}
            </Text>
            <View style={styles.actions}>
              <Button mode="contained-tonal" icon="content-copy" onPress={() => onCopy(invite.code)}>
                {STR.copy}
              </Button>
              <Button mode="text" textColor={theme.colors.error} icon="link-off" onPress={() => onRevoke(invite.id)}>
                {STR.projects.revokeInviteCta}
              </Button>
            </View>
          </>
        ) : (
          <Button
            mode="contained-tonal"
            icon="link-plus"
            loading={creating}
            onPress={onCreate}
            style={styles.create}
            contentStyle={styles.createContent}
          >
            {STR.projects.createInviteCta}
          </Button>
        )}
      </Card.Content>
    </Card>
  );
}

const styles = StyleSheet.create({
  card: { borderRadius: radius.lg },
  header: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.md },
  title: { fontWeight: '600' },
  codeBox: { borderRadius: radius.md, paddingVertical: spacing.md, alignItems: 'center', marginBottom: spacing.sm },
  code: { letterSpacing: 4, fontWeight: '700' },
  actions: { flexDirection: 'row', justifyContent: 'flex-end', alignItems: 'center', marginTop: spacing.sm, gap: spacing.sm },
  create: { alignSelf: 'flex-start', borderRadius: radius.md },
  createContent: { minHeight: 44 },
});
