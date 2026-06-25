$ErrorActionPreference = "Stop"
$projectDir = "C:\Users\Even\Desktop\Program"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "============================================"
Write-Host "  社区投资论坛 - 一键启动"
Write-Host "============================================"
Write-Host ""

# 1. 检查 MySQL
Write-Host "[1/4] 检查 MySQL 服务..."
$mysqlSvc = Get-Service -Name "MySQL80" -ErrorAction SilentlyContinue
if (-not $mysqlSvc -or $mysqlSvc.Status -ne "Running") {
    Write-Host "[警告] MySQL 未运行，尝试启动..."
    try { Start-Service -Name "MySQL80" } catch {
        Write-Host "[警告] MySQL 启动失败: $_"
        Write-Host "[警告] 将继续启动其他服务..."
    }
}
if ((Get-Service -Name "MySQL80" -ErrorAction SilentlyContinue).Status -eq "Running") {
    Write-Host "[完成] MySQL 服务运行中"
} else {
    Write-Host "[警告] MySQL 未运行，请手动启动"
}
Write-Host ""

# 2. 启动后端
Write-Host "[2/4] 启动后端服务 (Spring Boot, 端口 8080)..."
$env:JAVA_HOME = "C:\Program Files\BellSoft\LibericaJDK-17"
$env:MAVEN_HOME = "C:\Users\Even\apache-maven-3.9.6"
$env:PATH = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"

$backendProc = Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c cd /d `"$projectDir\backend`" && mvn spring-boot:run" `
    -WindowStyle Minimized -PassThru
Write-Host "[完成] 后端正在启动 (进程ID: $($backendProc.Id), 最小化窗口)"
Write-Host ""

# 3. 等待后端就绪
Write-Host "[3/4] 等待后端就绪 (最长等待 60 秒)..."
$ready = $false
for ($i = 1; $i -le 60; $i++) {
    Start-Sleep -Seconds 1
    try {
        $null = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/health" -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
        $ready = $true
        break
    } catch {}
    if ($i % 10 -eq 0) { Write-Host "  等待中... ${i}秒" }
}
if ($ready) {
    Write-Host "[完成] 后端已就绪"
} else {
    Write-Host "[警告] 后端可能仍在启动，将继续启动前端..."
}
Write-Host ""

# 4. 启动前端
Write-Host "[4/4] 启动前端服务 (Vite, 端口 3000)..."
$frontendProc = Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c cd /d `"$projectDir\frontend`" && npm run dev" `
    -WindowStyle Minimized -PassThru
Write-Host "[完成] 前端正在启动 (进程ID: $($frontendProc.Id), 最小化窗口)"
Write-Host ""

# 5. 等待前端就绪并打开浏览器
Write-Host "等待前端就绪..."
Start-Sleep -Seconds 5
Start-Process "http://localhost:3000"

Write-Host "============================================"
Write-Host "  全部服务已启动！"
Write-Host "  前端页面: http://localhost:3000"
Write-Host "  后端接口: http://localhost:8080"
Write-Host "============================================"
Write-Host ""
Write-Host "按任意键关闭此窗口（服务将继续运行）..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
