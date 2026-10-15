import { Stack, Text, Title } from '@mantine/core';

export function HomePage() {
  return (
    <Stack gap="xs">
      <Title order={1}>Welcome</Title>
      <Text c="dimmed">Manage and track your payments.</Text>
    </Stack>
  );
}
