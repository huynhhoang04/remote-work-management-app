import { router } from 'expo-router';
import React from 'react';
import { Image, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { useForm } from 'react-hook-form';
import { Card, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import logo from '@/assets/images/logo.png';
import { normalizeError } from '@/api/errors';
import { AppButton } from '@/components/AppButton';
import { ControlledField } from '@/components/ControlledField';
import { useSnackbar } from '@/components/SnackbarProvider';
import { useLogin } from '@/features/auth/hooks';
import { loginSchema, type LoginForm } from '@/schemas/auth';
import { radius, spacing } from '@/theme/tokens';
import { STR } from '@/utils/constants';
import { zodResolver } from '@/utils/zodResolver';

/**
 * Đăng nhập (FR-A02).
 * @reference docs/plan-app/01-phase-1.md §5.2
 */
export default function LoginScreen() {
  const theme = useTheme();
  const { show } = useSnackbar();
  const login = useLogin();

  const { control, handleSubmit, setError, formState } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      await login.mutateAsync(values);
      router.replace('/today');
    } catch (error) {
      const apiError = normalizeError(error);
      if (apiError.code === 'AUTH_INVALID_CREDENTIALS') {
        setError('password', { message: STR.auth.invalidCredentials });
      } else if (apiError.code === 'AUTH_RATE_LIMITED') {
        show(STR.auth.rateLimited);
      } else {
        show(apiError.message);
      }
    }
  });

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.flex}>
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
          <View style={styles.brand}>
            <Image source={logo} style={styles.logo} resizeMode="contain" />
            <Text variant="headlineMedium" style={styles.appName}>
              {STR.appName}
            </Text>
            <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant }}>
              {STR.auth.appTagline}
            </Text>
          </View>

          <Card mode="elevated" style={styles.card}>
            <Card.Content style={styles.cardContent}>
              <Text variant="titleLarge" style={styles.cardTitle}>
                {STR.auth.loginTitle}
              </Text>
              <Text variant="bodySmall" style={[styles.cardSubtitle, { color: theme.colors.onSurfaceVariant }]}>
                {STR.auth.loginSubtitle}
              </Text>

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
                secure
                leftIcon="lock-outline"
              />

              <AppButton
                label={STR.auth.loginCta}
                onPress={onSubmit}
                loading={login.isPending}
                style={styles.submit}
              />

              <Text
                variant="bodyMedium"
                onPress={() => router.push('/forgot-password')}
                style={[styles.link, { color: theme.colors.primary }]}
              >
                {STR.auth.forgotLink}
              </Text>
            </Card.Content>
          </Card>

          <View style={styles.footer}>
            <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant }}>
              {STR.auth.noAccount}{' '}
            </Text>
            <Text
              variant="bodyMedium"
              onPress={() => router.push('/register')}
              style={[styles.linkInline, { color: theme.colors.primary }]}
            >
              {STR.auth.registerCta}
            </Text>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  flex: { flex: 1 },
  container: { flexGrow: 1, justifyContent: 'center', padding: spacing.xl, gap: spacing.lg },
  brand: { alignItems: 'center', gap: spacing.sm },
  logo: { width: 108, height: 108, borderRadius: 26 },
  appName: { fontWeight: '700', letterSpacing: 0.2 },
  card: { borderRadius: radius.xl },
  cardContent: { padding: spacing.lg },
  cardTitle: { fontWeight: '700' },
  cardSubtitle: { marginTop: 2, marginBottom: spacing.lg },
  submit: { marginTop: spacing.sm },
  link: { textAlign: 'center', marginTop: spacing.md },
  linkInline: { fontWeight: '600' },
  footer: { flexDirection: 'row', justifyContent: 'center', alignItems: 'center' },
});
