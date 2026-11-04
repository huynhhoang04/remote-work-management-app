import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Avatar, Card, Icon, Text, useTheme } from 'react-native-paper';
import { radius, spacing } from '../theme/tokens';
import type { ProjectDto } from '../types/api';
import { colorFromString, formatDate, initials } from '../utils/format';
import { STR } from '../utils/constants';
import { RoleChip } from './RoleChip';

interface Props {
  project: ProjectDto;
  onPress: (project: ProjectDto) => void;
}

/**
 * Card một dự án trong danh sách.
 * @reference docs/plan-app/01-phase-1.md §4.2
 */
export function ProjectCard({ project, onPress }: Props) {
  const theme = useTheme();
  const avatarColor = colorFromString(project.name);

  return (
    <Card mode="elevated" style={styles.card} onPress={() => onPress(project)}>
      <Card.Content style={styles.content}>
        <View style={styles.row}>
          <Avatar.Text
            size={44}
            label={initials(project.name)}
            style={{ backgroundColor: `${avatarColor}1F` }}
            labelStyle={[styles.avatarLabel, { color: avatarColor }]}
          />
          <View style={styles.middle}>
            <Text variant="titleMedium" numberOfLines={2} style={styles.name}>
              {project.name}
            </Text>
            {project.description ? (
              <Text
                variant="bodySmall"
                numberOfLines={2}
                style={[styles.description, { color: theme.colors.onSurfaceVariant }]}
              >
                {project.description}
              </Text>
            ) : null}
          </View>
          <RoleChip role={project.role} />
        </View>

        <View style={[styles.footer, { borderTopColor: theme.colors.outline }]}>
          <View style={styles.footerItem}>
            <Icon source="calendar-blank-outline" size={14} color={theme.colors.onSurfaceVariant} />
            <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
              {STR.projects.createdOn} {formatDate(project.createdAt)}
            </Text>
          </View>
          <Icon source="chevron-right" size={18} color={theme.colors.onSurfaceVariant} />
        </View>
      </Card.Content>
    </Card>
  );
}

const styles = StyleSheet.create({
  card: { marginHorizontal: spacing.lg, marginBottom: spacing.md, borderRadius: radius.lg },
  content: { padding: spacing.lg },
  row: { flexDirection: 'row', alignItems: 'flex-start' },
  middle: { flex: 1, marginHorizontal: spacing.md },
  name: { fontWeight: '600', lineHeight: 22 },
  description: { marginTop: 2 },
  avatarLabel: { fontSize: 16, fontWeight: '700' },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderTopWidth: StyleSheet.hairlineWidth,
    marginTop: spacing.md,
    paddingTop: spacing.sm,
  },
  footerItem: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs },
});
