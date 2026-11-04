import { router, Stack, useLocalSearchParams } from 'expo-router';
import React, { useState } from 'react';
import { Alert, StyleSheet, View } from 'react-native';
import { IconButton, Menu, Text, useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { MemberRow } from '@/components/MemberRow';
import { RoleChip } from '@/components/RoleChip';
import { useSnackbar } from '@/components/SnackbarProvider';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import {
  useChangeMemberRole,
  useMembers,
  useProject,
  useRemoveMember,
} from '@/features/projects/hooks';
import { useCreateDirectConversation } from '@/features/chat/hooks';
import { useSession } from '@/store/session';
import { spacing } from '@/theme/tokens';
import type { MemberDto, ProjectRole } from '@/types/api';
import { STR } from '@/utils/constants';
import { FlatList } from 'react-native';

const ROLES: ProjectRole[] = ['ADMIN', 'MEMBER', 'VIEWER'];

/**
 * Danh sách thành viên + đổi vai trò/gỡ (Owner/Admin) + mở chat 1-1.
 * @reference docs/plan-app/02-phase-2.md §5.3
 */
export default function ProjectMembersScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const { show } = useSnackbar();
  const { user } = useSession();

  const project = useProject(id);
  const members = useMembers(id);
  const changeRole = useChangeMemberRole(id);
  const removeMember = useRemoveMember(id);
  const createDirect = useCreateDirectConversation();

  const canManage = project.data?.role === 'OWNER' || project.data?.role === 'ADMIN';

  const openChat = async (member: MemberDto) => {
    try {
      const conversation = await createDirect.mutateAsync(member.userId);
      router.push(`/chat/${conversation.id}`);
    } catch (error) {
      show(normalizeError(error).message);
    }
  };

  const onRemove = (member: MemberDto) => {
    Alert.alert(STR.projectsExtra.removeMember, STR.projectsExtra.removeMemberConfirm, [
      { text: STR.cancel, style: 'cancel' },
      {
        text: STR.common.remove,
        style: 'destructive',
        onPress: async () => {
          try {
            await removeMember.mutateAsync(member.userId);
            show(STR.projectsExtra.memberRemoved);
          } catch (error) {
            show(normalizeError(error).message);
          }
        },
      },
    ]);
  };

  const onRole = async (member: MemberDto, role: ProjectRole) => {
    try {
      await changeRole.mutateAsync({ userId: member.userId, role });
      show(STR.projectsExtra.roleChanged);
    } catch (error) {
      show(normalizeError(error).message);
    }
  };

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen options={{ title: `${STR.projects.membersTitle} (${members.data?.length ?? 0})` }} />
      {members.isPending ? (
        <SkeletonList count={5} />
      ) : members.isError ? (
        <StateView status="error" errorText={normalizeError(members.error).message} onRetry={() => members.refetch()} />
      ) : (
        <FlatList
          data={members.data ?? []}
          keyExtractor={(item) => item.userId}
          renderItem={({ item }) => (
            <MemberListItem
              member={item}
              canManage={canManage && item.role !== 'OWNER' && item.userId !== user?.id}
              onPress={() => {
                if (item.userId !== user?.id) void openChat(item);
              }}
              onRemove={() => onRemove(item)}
              onRole={(role) => onRole(item, role)}
            />
          )}
          contentContainerStyle={styles.list}
        />
      )}
    </View>
  );
}

function MemberListItem({
  member,
  canManage,
  onPress,
  onRemove,
  onRole,
}: {
  member: MemberDto;
  canManage: boolean;
  onPress: () => void;
  onRemove: () => void;
  onRole: (role: ProjectRole) => void;
}) {
  const [menuVisible, setMenuVisible] = useState(false);

  return (
    <MemberRow
      member={member}
      onPress={onPress}
      right={
        <View style={styles.right}>
          <RoleChip role={member.role} />
          {canManage ? (
            <Menu
              visible={menuVisible}
              onDismiss={() => setMenuVisible(false)}
              anchor={<IconButton icon="dots-vertical" size={18} onPress={() => setMenuVisible(true)} />}
            >
              <Menu.Item title={STR.projectsExtra.changeRole} disabled />
              {ROLES.map((role) => (
                <Menu.Item key={role} title={(STR.roles as Record<string, string>)[role]} onPress={() => { setMenuVisible(false); onRole(role); }} />
              ))}
              <Menu.Item
                title={STR.projectsExtra.removeMember}
                onPress={() => {
                  setMenuVisible(false);
                  onRemove();
                }}
              />
            </Menu>
          ) : null}
        </View>
      }
    />
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  list: { paddingVertical: spacing.sm },
  right: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs },
});
