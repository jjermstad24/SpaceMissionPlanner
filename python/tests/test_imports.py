import spacemissionplanner
from spacemissionplanner.wrappers import native_extension_status
from spacemissionplanner.wrappers.backend import NativeExtensionStatus


def test_version_defined() -> None:
    assert spacemissionplanner.__version__


def test_native_extension_not_built_yet() -> None:
    info = native_extension_status("spacemissionplanner._native")
    assert info.status in (NativeExtensionStatus.MISSING, NativeExtensionStatus.LOADED, NativeExtensionStatus.ERROR)
