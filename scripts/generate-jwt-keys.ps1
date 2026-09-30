# Execute localmente em um terminal privado. Nunca salve a saída no repositório.
$rsaKeyPair = [System.Security.Cryptography.RSA]::Create(2048)
try {
    $privateKey = [Convert]::ToBase64String($rsaKeyPair.ExportPkcs8PrivateKey())
    $publicKey = [Convert]::ToBase64String($rsaKeyPair.ExportSubjectPublicKeyInfo())
    Write-Output "JWT_PRIVATE_KEY=$privateKey"
    Write-Output "JWT_PUBLIC_KEY=$publicKey"
} finally {
    $rsaKeyPair.Dispose()
}
