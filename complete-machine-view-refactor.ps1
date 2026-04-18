# PowerShell script to complete machine view refactoring
# Run this to apply State pattern to all remaining machine views

$views = @(
    "MachineRecipeView",
    "MachineStorageView",
    "MachineTrustView",
    "MachineUpgradeView"
)

Write-Host "Machine View Refactoring Script" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "This script will refactor the following views to use State pattern:" -ForegroundColor Yellow
$views | ForEach-Object { Write-Host "  - $_" -ForegroundColor White }
Write-Host ""
Write-Host "Pattern to apply:" -ForegroundColor Green
Write-Host "1. Change constructor to no-arg: public ViewName() { super(); }" -ForegroundColor Gray
Write-Host "2. Change fields to State: private final State<Type> field = initialState(""key"");" -ForegroundColor Gray
Write-Host "3. Add import: import me.devnatan.inventoryframework.state.State;" -ForegroundColor Gray
Write-Host "4. Fix all .get() calls to include context parameter" -ForegroundColor Gray
Write-Host ""
Write-Host "Manual steps required:" -ForegroundColor Yellow
Write-Host "For each view file, replace:" -ForegroundColor White
Write-Host "  - Constructor with parameters → no-arg constructor" -ForegroundColor Gray
Write-Host "  - private final Type field → private final State<Type> field = initialState(""key"")" -ForegroundColor Gray
Write-Host "  - machine. → machine.get(render). in render methods" -ForegroundColor Gray
Write-Host "  - machine. → machine.get(click). in click handlers" -ForegroundColor Gray
Write-Host "  - machineService. → machineService.get(context). everywhere" -ForegroundColor Gray
Write-Host ""
Write-Host "After refactoring, run: ./gradlew :RDQ:rdq-common:build -x test" -ForegroundColor Cyan
