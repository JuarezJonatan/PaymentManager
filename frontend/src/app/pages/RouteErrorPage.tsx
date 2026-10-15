import { Button, Center, Stack, Text, Title } from '@mantine/core';

export function RouteErrorPage() {
  return (
    <Center mih="100vh" p="md">
      <Stack align="center" gap="sm">
        <Title order={1}>Something went wrong</Title>
        <Text c="dimmed">An unexpected error occurred. Reload the page to try again.</Text>
        <Button onClick={() => window.location.reload()} variant="light">
          Reload
        </Button>
      </Stack>
    </Center>
  );
}
