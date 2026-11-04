import { router } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { FAB, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { normalizeError } from '@/api/errors';
import { AppButton } from '@/components/AppButton';
import { ProjectCard } from '@/components/ProjectCard';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { TabHeader } from '@/components/TabHeader';
import { useProjects } from '@/features/projects/hooks';
import { spacing } from '@/theme/tokens';
import type { ProjectDto } from '@/types/api';
import { STR } from '@/utils/constants';

/**
 * Danh sách dự án (FR-B02) — skeleton / empty / error / content.
 * @reference docs/plan-app/01-phase-1.md §5.5
 */
export default function ProjectListScreen() {
  const theme = useTheme();
  const projects = useProjects();
  const [refreshing, setRefreshing] = useState(false);

  const openProject = useCallback((project: ProjectDto) => {
    router.push(`/projects/${project.id}`);
  }, []);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await projects.refetch();
    } finally {
      setRefreshing(false);
    }
  }, [projects]);

  return (
    <SafeAreaView style={styles.safe} edges={['left', 'right']}>
      <TabHeader
        title={STR.projects.title}
        actions={[
          {
            icon: 'account-multiple-plus-outline',
            onPress: () => router.push('/projects/join'),
            accessibilityLabel: STR.projects.joinCta,
          },
        ]}
      />

      {projects.isPending ? (
        <View style={styles.skeleton}>
          <SkeletonList count={4} />
        </View>
      ) : projects.isError ? (
        <StateView status="error" errorText={normalizeError(projects.error).message} onRetry={() => projects.refetch()} />
      ) : !projects.data || projects.data.length === 0 ? (
        <StateView status="empty" emptyHint={STR.projects.emptyHint}>
          <AppButton label={STR.projects.createCta} icon="plus" onPress={() => router.push('/projects/new')} />
          <AppButton
            label={STR.projects.joinCta}
            icon="account-multiple-plus-outline"
            variant="outline"
            onPress={() => router.push('/projects/join')}
          />
        </StateView>
      ) : (
        <>
          <View style={styles.listHeader}>
            <Text variant="titleSmall">{STR.projects.listHeader}</Text>
            <Text variant="labelMedium" style={{ color: theme.colors.onSurfaceVariant }}>
              {projects.data.length}
            </Text>
          </View>
          <FlatList
            data={projects.data}
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => <ProjectCard project={item} onPress={openProject} />}
            contentContainerStyle={styles.list}
            showsVerticalScrollIndicator={false}
            refreshControl={
              <RefreshControl
                refreshing={refreshing}
                onRefresh={onRefresh}
                tintColor={theme.colors.primary}
                colors={[theme.colors.primary]}
              />
            }
          />
        </>
      )}

      <FAB icon="plus" label={STR.projects.createCta} style={styles.fab} onPress={() => router.push('/projects/new')} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  headerActions: { flexDirection: 'row' },
  skeleton: { marginTop: spacing.sm },
  listHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.md,
    paddingBottom: spacing.sm,
  },
  list: { paddingBottom: 120 },
  fab: { position: 'absolute', right: spacing.lg, bottom: spacing.xl },
});
