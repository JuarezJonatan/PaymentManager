import { Button, Group, Modal, Text } from '@mantine/core';

type RefundPaymentModalProps = {
  readonly opened: boolean;
  readonly formattedAmount: string;
  readonly isRefunding: boolean;
  readonly onConfirm: () => void;
  readonly onClose: () => void;
};

export function RefundPaymentModal({
  opened,
  formattedAmount,
  isRefunding,
  onConfirm,
  onClose,
}: RefundPaymentModalProps) {
  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title="Refund payment"
      closeOnClickOutside={!isRefunding}
      closeOnEscape={!isRefunding}
      withCloseButton={!isRefunding}
    >
      <Text size="sm">
        The full amount of {formattedAmount} will be refunded. This action cannot be undone.
      </Text>
      <Group justify="flex-end" mt="lg">
        <Button variant="default" onClick={onClose} disabled={isRefunding}>
          Cancel
        </Button>
        <Button color="red" onClick={onConfirm} loading={isRefunding}>
          Confirm refund
        </Button>
      </Group>
    </Modal>
  );
}
