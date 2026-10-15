import { Alert, Button, Text } from '@mantine/core';

type ErrorStateProps = {
  readonly title: string;
  readonly message: string;
  readonly onRetry: () => void;
};

export function ErrorState({ title, message, onRetry }: ErrorStateProps) {
  return (
    <Alert color="red" title={title} role="alert">
      <Text size="sm" mb="sm">
        {message}
      </Text>
      <Button size="xs" variant="light" color="red" onClick={onRetry}>
        Retry
      </Button>
    </Alert>
  );
}
