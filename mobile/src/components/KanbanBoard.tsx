import React, { useCallback, useRef } from 'react';
import { ScrollView, StyleSheet, View, useWindowDimensions } from 'react-native';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import Animated, { runOnJS, useAnimatedStyle, useSharedValue, withSpring } from 'react-native-reanimated';
import { Icon, Text, useTheme } from 'react-native-paper';
import { radius, spacing, statusColors } from '../theme/tokens';
import type { TaskDto, TaskStatus } from '../types/api';
import { STR } from '../utils/constants';
import { TaskCard } from './TaskCard';

const STATUSES: TaskStatus[] = ['TODO', 'DOING', 'REVIEW', 'DONE'];

interface BoardProps {
  tasks: TaskDto[];
  onOpenTask: (task: TaskDto) => void;
  onMoveTask: (task: TaskDto, status: TaskStatus, position: number) => void;
  readOnly?: boolean;
}

interface ColumnBounds {
  x: number;
  width: number;
}

/**
 * Kanban 4 cột + kéo thả đổi trạng thái.
 * Kéo sau khi giữ 250ms; thả vào cột khác sẽ gọi API position (cuối cột).
 * @reference docs/plan-app/02-phase-2.md §5.4
 */
export function KanbanBoard({ tasks, onOpenTask, onMoveTask, readOnly = false }: BoardProps) {
  const theme = useTheme();
  const { width } = useWindowDimensions();
  const columnWidth = Math.min(300, Math.max(240, width * 0.8));
  const columnRefs = useRef<Record<string, React.ComponentRef<typeof View> | null>>({});
  const columnBounds = useRef<Record<string, ColumnBounds>>({});

  const measureColumns = useCallback(() => {
    STATUSES.forEach((status) => {
      const node = columnRefs.current[status] as unknown as
        | { measureInWindow?: (callback: (x: number, y: number, width: number, height: number) => void) => void }
        | null;
      node?.measureInWindow?.((x, _y, columnWidthValue) => {
        columnBounds.current[status] = { x, width: columnWidthValue };
      });
    });
  }, []);

  const handleDrop = useCallback(
    (task: TaskDto, absoluteX: number) => {
      const target = STATUSES.find((status) => {
        const bounds = columnBounds.current[status];
        return bounds && absoluteX >= bounds.x && absoluteX <= bounds.x + bounds.width;
      });
      if (!target || target === task.status) return;

      const inTarget = tasks.filter((item) => item.status === target && item.id !== task.id);
      const nextPosition = inTarget.length
        ? Math.max(...inTarget.map((item) => item.position)) + 1000
        : 1000;
      onMoveTask(task, target, nextPosition);
    },
    [onMoveTask, tasks],
  );

  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={styles.board}
      keyboardShouldPersistTaps="handled"
    >
      {STATUSES.map((status) => {
        const columnTasks = tasks
          .filter((task) => task.status === status)
          .sort((a, b) => a.position - b.position);

        return (
          <View
            key={status}
            ref={(node) => {
              columnRefs.current[status] = node;
            }}
            style={[styles.column, { width: columnWidth }]}
          >
            <View style={[styles.columnHeader, { backgroundColor: `${statusColors[status]}14` }]}>
              <View style={[styles.dot, { backgroundColor: statusColors[status] }]} />
              <Text variant="titleSmall" style={styles.columnTitle}>
                {(STR.tasks.statuses as Record<string, string>)[status] ?? status}
              </Text>
              <Text variant="labelMedium" style={styles.count}>
                {columnTasks.length}
              </Text>
            </View>

            <ScrollView
              showsVerticalScrollIndicator={false}
              contentContainerStyle={styles.columnBody}
              nestedScrollEnabled
              keyboardShouldPersistTaps="handled"
            >
              {columnTasks.length === 0 ? (
                <View style={styles.emptyColumn}>
                  <Icon source="tray" size={22} color={theme.colors.onSurfaceVariant} />
                  <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
                    Trống
                  </Text>
                </View>
              ) : (
                columnTasks.map((task) => (
                  <DraggableTask
                    key={task.id}
                    task={task}
                    readOnly={readOnly}
                    onOpen={() => onOpenTask(task)}
                    onDragStart={measureColumns}
                    onDrop={handleDrop}
                  />
                ))
              )}
            </ScrollView>
          </View>
        );
      })}
    </ScrollView>
  );
}

interface DraggableProps {
  task: TaskDto;
  readOnly: boolean;
  onOpen: () => void;
  onDragStart: () => void;
  onDrop: (task: TaskDto, absoluteX: number) => void;
}

function DraggableTask({ task, readOnly, onOpen, onDragStart, onDrop }: DraggableProps) {
  const theme = useTheme();
  const translationX = useSharedValue(0);
  const translationY = useSharedValue(0);
  const scale = useSharedValue(1);
  const dragging = useSharedValue(0);

  const tap = Gesture.Tap().maxDuration(250).onEnd(() => {
    runOnJS(onOpen)();
  });

  const pan = Gesture.Pan()
    .enabled(!readOnly)
    .activateAfterLongPress(250)
    .onStart(() => {
      dragging.value = withSpring(1);
      scale.value = withSpring(1.03);
      runOnJS(onDragStart)();
    })
    .onUpdate((event) => {
      translationX.value = event.translationX;
      translationY.value = event.translationY;
    })
    .onEnd((event) => {
      runOnJS(onDrop)(task, event.absoluteX);
      translationX.value = withSpring(0);
      translationY.value = withSpring(0);
      scale.value = withSpring(1);
      dragging.value = withSpring(0);
    })
    .onFinalize(() => {
      translationX.value = withSpring(0);
      translationY.value = withSpring(0);
      scale.value = withSpring(1);
      dragging.value = withSpring(0);
    });

  const gesture = Gesture.Exclusive(pan, tap);

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [
      { translateX: translationX.value },
      { translateY: translationY.value },
      { scale: scale.value },
    ],
    zIndex: dragging.value === 1 ? 50 : 0,
    opacity: dragging.value === 1 ? 0.96 : 1,
    elevation: dragging.value === 1 ? 12 : 0,
  }));

  return (
    <GestureDetector gesture={gesture}>
      <Animated.View style={[styles.draggable, animatedStyle]}>
        <TaskCard task={task} compact />
      </Animated.View>
    </GestureDetector>
  );
}

const styles = StyleSheet.create({
  board: { paddingHorizontal: spacing.md, paddingBottom: spacing.xl, gap: spacing.md },
  column: { alignSelf: 'flex-start', maxHeight: '100%' },
  columnHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: radius.md,
  },
  dot: { width: 8, height: 8, borderRadius: 4 },
  columnTitle: { fontWeight: '600', flex: 1 },
  count: { opacity: 0.7 },
  columnBody: { paddingTop: spacing.sm, paddingBottom: spacing.xl, minHeight: 180 },
  emptyColumn: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing.xl,
    gap: spacing.xs,
  },
  emptyText: { color: 'transparent' }, // Được ghi đè bằng theme inline
  draggable: { borderRadius: radius.md },
});
