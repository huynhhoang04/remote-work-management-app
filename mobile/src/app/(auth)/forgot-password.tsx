import { router } from 'expo-router';
import React from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet } from 'react-native';
import { useForm } from 'react-hook-form';
import { Card, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { normalizeError } from '@/api/errors';
import { AppButton } from '@/components/AppButton';
import { ControlledField } from '@/components/ControlledField';
import { useSnackbar } from '@/components/SnackbarProvider';
import { useForgotPassword } from '@/features/auth/hooks';
import { forgotPasswordSchema, type ForgotPasswordForm } from '@/schemas/auth';
import { radius, spacing } from '@/theme/tokens';
import { STR } from '@/utils/constants';
import { zodResolver } from '@/utils/zodResolver';

/**
 * Quên mật khẩu (FR-A03) — backend luôn 204 để không lộ email tồn tại.
 * @reference docs/plan-app/01-phase-1.md §5.4
 */
export default function ForgotPasswordScreen() {
  const theme = useTheme();
  const { show } = useSnackbar();
  const forgot = useForgotPassword();

  const { control, handleSubmit, formState } = useForm<ForgotPasswordForm>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      await forgot.mutateAsync(values.email.trim());
      show(STR.auth.forgotSent);
      router.replace('/login');
    } catch (error) {
      show(normalizeError(error).message);
    }
  });

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.flex}>
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
          <Card mode="elevated" style={styles.card}>
            <Card.Content style={styles.cardContent}>
              <Text variant="titleLarge" style={styles.cardTitle}>
                {STR.auth.forgotTitle}
              </Text>
              <Text variant="bodySmall" style={[styles.cardSubtitle, { color: theme.colors.onSurfaceVariant }]}>
                {STR.auth.forgotSubtitle}
              </Text>

              <ControlledField
                control={control}
                name="email"
                label={STR.auth.email}
                error={formState.errors.email?.message}
                keyboardType="email-address"
                leftIcon="email-outline"
              />

              <AppButton
                label={STR.auth.forgotCta}
                onPress={onSubmit}
                loading={forgot.isPending}
                style={styles.submit}
              />

              <Text
                variant="bodyMedium"
                onPress={() => router.replace('/login')}
                style={[styles.link, { color: theme.colors.primary }]}
              >
                {STR.auth.backToLogin}
              </Text>
            </Card.Content>
          </Card>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  flex: { flex: 1 },
  container: { flexGrow: 1, justifyContent: 'center', padding: spacing.xl },
  card: { borderRadius: radius.xl },
  cardContent: { padding: spacing.lg },
  cardTitle: { fontWeight: '700' },
  cardSubtitle: { marginTop: 2, marginBottom: spacing.lg },
  submit: { marginTop: spacing.sm },
  link: { textAlign: 'center', marginTop: spacing.md },
});
