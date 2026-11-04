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
import { useRegister } from '@/features/auth/hooks';
import { registerSchema, type RegisterForm } from '@/schemas/auth';
import { radius, spacing } from '@/theme/tokens';
import { STR } from '@/utils/constants';
import { zodResolver } from '@/utils/zodResolver';

/**
 * Đăng ký tài khoản (FR-A01) — thành công tự đăng nhập.
 * @reference docs/plan-app/01-phase-1.md §5.3
 */
export default function RegisterScreen() {
  const theme = useTheme();
  const { show } = useSnackbar();
  const register = useRegister();

  const { control, handleSubmit, setError, formState } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: { fullName: '', email: '', password: '', confirmPassword: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      await register.mutateAsync({
        fullName: values.fullName.trim(),
        email: values.email,
        password: values.password,
      });
      show(STR.auth.registerSuccess);
      router.replace('/today');
    } catch (error) {
      const apiError = normalizeError(error);
      if (apiError.code === 'USER_EMAIL_EXISTS') {
        setError('email', { message: STR.auth.emailExists });
      } else {
        show(apiError.message);
      }
    }
  });

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.flex}>
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
          <Card mode="elevated" style={styles.card}>
            <Card.Content style={styles.cardContent}>
              <Text variant="titleLarge" style={styles.cardTitle}>
                {STR.auth.registerTitle}
              </Text>
              <Text variant="bodySmall" style={[styles.cardSubtitle, { color: theme.colors.onSurfaceVariant }]}>
                {STR.auth.registerSubtitle}
              </Text>

              <ControlledField
                control={control}
                name="fullName"
                label={STR.auth.fullName}
                error={formState.errors.fullName?.message}
                autoCapitalize="words"
                maxLength={100}
                leftIcon="account-outline"
              />
              <ControlledField
                control={control}
                name="email"
                label={STR.auth.email}
                error={formState.errors.email?.message}
                keyboardType="email-address"
                leftIcon="email-outline"
              />
              <ControlledField
                control={control}
                name="password"
                label={STR.auth.password}
                error={formState.errors.password?.message}
                helperText={STR.auth.passwordHint}
                secure
                leftIcon="lock-outline"
              />
              <ControlledField
                control={control}
                name="confirmPassword"
                label={STR.auth.confirmPassword}
                error={formState.errors.confirmPassword?.message}
                secure
                leftIcon="lock-check-outline"
              />

              <AppButton
                label={STR.auth.registerCta}
                onPress={onSubmit}
                loading={register.isPending}
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
