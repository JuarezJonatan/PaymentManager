import { Center, Loader } from '@mantine/core';

type LoadingStateProps = {
  readonly label: string;
};

export function LoadingState({ label }: LoadingStateProps) {
  return (
    <Center py="xl">
      <Loader role="status" aria-label={label} />
    </Center>
  );
}
