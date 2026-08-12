$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$modelRoot = Join-Path $root 'models'
$archive = Join-Path $modelRoot 'zh-en-small.tar.bz2'
$release = 'https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-16.tar.bz2'

New-Item -ItemType Directory -Force -Path $modelRoot | Out-Null
Write-Host 'Downloading small bilingual zh-en streaming Zipformer...'
Invoke-WebRequest -Uri $release -OutFile $archive

if (Get-Command tar.exe -ErrorAction SilentlyContinue) {
    tar.exe -xf $archive -C $modelRoot
} else {
    throw 'Windows tar.exe was not found. Use Windows 10/11 or install a tar utility.'
}
Remove-Item $archive -Force

$src = Join-Path $modelRoot 'sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-16'
$required = @(
    'encoder-epoch-99-avg-1.int8.onnx',
    'decoder-epoch-99-avg-1.onnx',
    'joiner-epoch-99-avg-1.int8.onnx',
    'tokens.txt',
    'bpe.model'
)
foreach ($f in $required) {
    if (-not (Test-Path (Join-Path $src $f))) { throw "Missing model file: $f" }
}

Write-Host "Model ready: $src"
Get-ChildItem $src | Select-Object Name, Length
