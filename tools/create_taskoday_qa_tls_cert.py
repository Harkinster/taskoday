#!/usr/bin/env python3
"""Create the emulator-only TLS identity. The private key is kept outside the repository."""

from __future__ import annotations

import hashlib
import ipaddress
import os
from pathlib import Path
from datetime import datetime, timedelta

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import ExtendedKeyUsageOID, NameOID


def main() -> None:
    local_app_data = os.environ.get("LOCALAPPDATA")
    if not local_app_data:
        raise SystemExit("LOCALAPPDATA is required on Windows")
    private_dir = Path(local_app_data) / "TaskodayQA" / "tls"
    private_dir.mkdir(parents=True, exist_ok=True)
    key_path = private_dir / "taskoday-qa-emulator.key.pem"
    cert_path = private_dir / "taskoday-qa-emulator.crt.pem"
    public_copy = Path(__file__).resolve().parents[1] / "app/src/qa/res/raw/taskoday_qa_ca.pem"

    if key_path.exists() or cert_path.exists():
        if not key_path.exists() or not cert_path.exists():
            raise SystemExit(f"Incomplete QA TLS identity in {private_dir}; refusing to overwrite it")
        cert_pem = cert_path.read_bytes()
    else:
        key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "Taskoday QA Android Emulator")])
        builder = (
            x509.CertificateBuilder()
            .subject_name(name)
            .issuer_name(name)
            .public_key(key.public_key())
            .serial_number(x509.random_serial_number())
            .not_valid_before(datetime.utcnow() - timedelta(minutes=5))
            .not_valid_after(datetime.utcnow() + timedelta(days=1825))
            .add_extension(x509.BasicConstraints(ca=True, path_length=0), critical=True)
            .add_extension(
                x509.SubjectAlternativeName(
                    [
                        x509.IPAddress(ipaddress.ip_address("10.0.2.2")),
                        x509.IPAddress(ipaddress.ip_address("127.0.0.1")),
                        x509.DNSName("localhost"),
                    ]
                ),
                critical=False,
            )
            .add_extension(
                x509.KeyUsage(
                    digital_signature=True,
                    content_commitment=False,
                    key_encipherment=True,
                    data_encipherment=False,
                    key_agreement=False,
                    key_cert_sign=True,
                    crl_sign=True,
                    encipher_only=False,
                    decipher_only=False,
                ),
                critical=True,
            )
            .add_extension(x509.ExtendedKeyUsage([ExtendedKeyUsageOID.SERVER_AUTH]), critical=False)
        )
        cert = builder.sign(key, hashes.SHA256())
        key_path.write_bytes(
            key.private_bytes(
                serialization.Encoding.PEM,
                serialization.PrivateFormat.PKCS8,
                serialization.NoEncryption(),
            )
        )
        cert_pem = cert.public_bytes(serialization.Encoding.PEM)
        cert_path.write_bytes(cert_pem)

    if public_copy.exists() and public_copy.read_bytes() != cert_pem:
        raise SystemExit(f"{public_copy} contains another QA CA; inspect it before changing trust")
    public_copy.parent.mkdir(parents=True, exist_ok=True)
    public_copy.write_bytes(cert_pem)
    fingerprint = hashlib.sha256(x509.load_pem_x509_certificate(cert_pem).public_bytes(serialization.Encoding.DER)).hexdigest()
    print(f"QA TLS private key: {key_path}")
    print(f"QA TLS public trust anchor: {public_copy}")
    print(f"QA TLS certificate SHA-256: {fingerprint}")


if __name__ == "__main__":
    main()
