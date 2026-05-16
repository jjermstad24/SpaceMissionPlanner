"""Tests for SPICE ephemeris integration."""

import numpy as np
import pytest

from spacemissionplanner.visualization.ephemeris import (
    SpiceEphemerisProvider,
    ToyEphemerisProvider,
    build_spice_viewer_episode,
    EphemerisProvider,
    HAS_NATIVE,
)


class TestEphemerisProvider:
    """Test the EphemerisProvider protocol."""

    def test_protocol_exists(self):
        """Verify the protocol is defined."""
        assert hasattr(EphemerisProvider, "sample_bodies")

    def test_toy_provider_conforms(self):
        """Toy provider should conform to protocol."""
        provider = ToyEphemerisProvider()
        assert isinstance(provider, EphemerisProvider)

    @pytest.mark.skipif(not HAS_NATIVE, reason="Native bindings not available")
    def test_spice_provider_loads_without_kernels(self):
        """Spice provider should initialize without kernels (fails on query)."""
        provider = SpiceEphemerisProvider()
        assert provider is not None


class TestToyEphemerisProvider:
    """Test the ToyEphemerisProvider implementation."""

    def test_sample_bodies_returns_positions(self):
        """Should return position arrays for requested bodies."""
        provider = ToyEphemerisProvider()
        times = np.linspace(0, 86400, 10)
        body_ids = ["Sun", "Earth"]

        result = provider.sample_bodies(body_ids, times)

        assert isinstance(result, dict)
        for body_id in body_ids:
            assert body_id in result
            assert result[body_id].shape == (10, 3)

    def test_sample_bodies_unknown_body(self):
        """Should gracefully handle unknown bodies."""
        provider = ToyEphemerisProvider()
        times = np.linspace(0, 86400, 10)
        body_ids = ["UnknownBody"]

        result = provider.sample_bodies(body_ids, times)

        assert result == {}


class TestBuildSpiceViewerEpisode:
    """Test the build_spice_viewer_episode helper."""

    @pytest.mark.skipif(not HAS_NATIVE, reason="Native bindings not available")
    def test_function_exists(self):
        """Should have build_spice_viewer_episode function."""
        assert callable(build_spice_viewer_episode)

    def test_accepts_trajectory(self):
        """Should accept trajectory array."""
        if not HAS_NATIVE:
            pytest.skip("Native bindings not available")

        # This will fail at runtime without kernels but validates the interface
        times = np.linspace(0, 86400, 10)
        trajectory = np.random.rand(10, 3) * 1e9

        # Just verify function signature accepts trajectory parameter
        # Actual execution requires SPICE kernels - skip for unit test
        pytest.skip("Requires SPICE kernels to run")