package com.portpoc.recipient.domain.ports;

import java.io.InputStream;
import java.util.Optional;

public interface ReceiptStorage {
    String uploadReceipt(String key, InputStream content);

    Optional<InputStream> downloadReceipt(String key);
}
