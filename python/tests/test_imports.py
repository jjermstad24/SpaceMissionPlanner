import spacemissionplanner
from spacemissionplanner.wrappers import native_extension_status
from spacemissionplanner.wrappers.backend import NativeExtensionStatus


def test_version_defined() -> None:
    assert spacemissionplanner.__version__


def test_native_extension_status() -> None:
    info = native_extension_status()
    assert info.status in (NativeExtensionStatus.MISSING, NativeExtensionStatus.LOADED, NativeExtensionStatus.ERROR)
    if info.status == NativeExtensionStatus.LOADED:
        assert info.module_name == "spacemissionplanner.spacemissionplanner_native"
