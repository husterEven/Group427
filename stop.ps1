$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "============================================"
Write-Host "  社区投资论坛 - 停止服务"
Write-Host "============================================"
Write-Host ""

# 停止后端 (端口 8080)
Write-Host "停止后端服务 (端口 8080)..."
$pids = (netstat -ano | Select-String ":8080" | Select-String "LISTENING").Line -replace ".*LISTENING\s+(\d+).*", '$1' | Select-Object -Unique
if ($pids) {
    foreach ($pid in $pids) {
        Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
        Write-Host "  已终止进程 ID: $pid"
    }
    Write-Host "[完成] 后端已停止"
} else {
    Write-Host "  未找到后端进程"
}

# 停止前端 (端口 3000)
Write-Host "停止前端服务 (端口 3000)..."
$pids = (netstat -ano | Select-String ":3000" | Select-String "LISTENING").Line -replace ".*LISTENING\s+(\d+).*", '$1' | Select-Object -Unique
if ($pids) {
    foreach ($pid in $pids) {
        Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
        Write-Host "  已终止进程 ID: $pid"
    }
    Write-Host "[完成] 前端已停止"
} else {
    Write-Host "  未找到前端进程"
}

Write-Host ""
Write-Host "所有服务已停止"
Write-Host "按任意键关闭..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
