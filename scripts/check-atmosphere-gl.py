#!/usr/bin/env python3
"""Compila, enlaza y dibuja el shader real con EGL/OpenGL sin ventana (Mesa en Linux).

No sustituye una partida con Minecraft ni verifica compatibilidad con paquetes de shaders.
Solo requiere Python 3, libEGL.so.1 y libGL.so.1; no instala dependencias globales.
"""
import ctypes as c
import os
from pathlib import Path


def bind(lib, name, result, *args):
    fn = getattr(lib, name)
    fn.restype, fn.argtypes = result, list(args)
    return fn


def check():
    # La prueba no escribe en la caché de usuario de la máquina anfitriona.
    os.environ.setdefault("MESA_SHADER_CACHE_DISABLE", "true")
    egl, gl = c.CDLL("libEGL.so.1"), c.CDLL("libGL.so.1")
    pointer, integer, uint, number = c.c_void_p, c.c_int, c.c_uint, c.c_float
    display = bind(egl, "eglGetPlatformDisplay", pointer, uint, pointer, pointer)(0x31DD, None, None)
    major, minor = integer(), integer()
    assert bind(egl, "eglInitialize", uint, pointer, pointer, pointer)(display, c.byref(major), c.byref(minor)), "EGL no disponible"
    assert bind(egl, "eglBindAPI", uint, uint)(0x30A2)
    attrs = (integer * 13)(0x3033, 1, 0x3040, 8, 0x3024, 8, 0x3023, 8, 0x3022, 8, 0x3021, 8, 0x3038)
    config, count = pointer(), integer()
    assert bind(egl, "eglChooseConfig", uint, pointer, pointer, pointer, integer, pointer)(display, attrs, c.byref(config), 1, c.byref(count)) and count.value
    surface_attrs = (integer * 5)(0x3057, 16, 0x3056, 16, 0x3038)
    surface = bind(egl, "eglCreatePbufferSurface", pointer, pointer, pointer, pointer)(display, config, surface_attrs)
    context_attrs = (integer * 7)(0x3098, 3, 0x30FB, 2, 0x30FD, 1, 0x3038)
    context = bind(egl, "eglCreateContext", pointer, pointer, pointer, pointer, pointer)(display, config, None, context_attrs)
    assert context and surface
    assert bind(egl, "eglMakeCurrent", uint, pointer, pointer, pointer, pointer)(display, surface, surface, context)
    try:
        text = bind(gl, "glGetString", c.c_char_p, uint)
        print("OpenGL:", text(0x1F02).decode(), "·", text(0x1F01).decode())
        create = bind(gl, "glCreateShader", uint, uint)
        source = bind(gl, "glShaderSource", None, uint, integer, pointer, pointer)
        compile_shader = bind(gl, "glCompileShader", None, uint)
        shader_status = bind(gl, "glGetShaderiv", None, uint, uint, pointer)
        shader_log = bind(gl, "glGetShaderInfoLog", None, uint, integer, pointer, pointer)
        root = Path(__file__).resolve().parent.parent / "src/main/resources/assets/galaxycraft/shaders/core"
        shaders = []
        for suffix, kind in [("vsh", 0x8B31), ("fsh", 0x8B30)]:
            shader = create(kind)
            content = c.c_char_p((root / f"atmosphere.{suffix}").read_bytes())
            source(shader, 1, c.byref(content), None)
            compile_shader(shader)
            status, log = integer(), c.create_string_buffer(8192)
            shader_status(shader, 0x8B81, c.byref(status))
            shader_log(shader, len(log), None, log)
            assert status.value, log.value.decode()
            shaders.append(shader)
        program = bind(gl, "glCreateProgram", uint)()
        for shader in shaders:
            bind(gl, "glAttachShader", None, uint, uint)(program, shader)
        for index, name in enumerate([b"Position", b"UV0", b"Color"]):
            bind(gl, "glBindAttribLocation", None, uint, uint, c.c_char_p)(program, index, name)
        bind(gl, "glLinkProgram", None, uint)(program)
        status, log = integer(), c.create_string_buffer(8192)
        bind(gl, "glGetProgramiv", None, uint, uint, pointer)(program, 0x8B82, c.byref(status))
        bind(gl, "glGetProgramInfoLog", None, uint, integer, pointer, pointer)(program, len(log), None, log)
        assert status.value, log.value.decode()
        bind(gl, "glUseProgram", None, uint)(program)
        location = bind(gl, "glGetUniformLocation", integer, uint, c.c_char_p)
        def scalar(name, value):
            bind(gl, "glUniform1f", None, integer, number)(location(program, name.encode()), value)
        def vector(name, values):
            bind(gl, "glUniform3f", None, integer, number, number, number)(location(program, name.encode()), *values)
        identity = (number * 16)(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1)
        for name in [b"ProjMat", b"ModelViewMat"]:
            bind(gl, "glUniformMatrix4fv", None, integer, integer, c.c_ubyte, pointer)(location(program, name), 1, 0, identity)
        bind(gl, "glUniform4f", None, integer, number, number, number, number)(location(program, b"ColorModulator"), 1, 1, 1, 1)
        bind(gl, "glUniform1i", None, integer, integer)(location(program, b"Samples"), 4)
        for name, value in {"PlanetRadius":6371, "AtmosphereHeight":0, "ObserverHeight":.002, "ScaleR":8, "ScaleM":1.2, "MieG":.76}.items():
            scalar(name, value)
        vector("SunDirection", (0, 1, 0))
        for name in ["BetaR", "BetaM", "Absorption"]:
            vector(name, (0, 0, 0))
        vao, vbo, texture = uint(), uint(), uint()
        bind(gl, "glGenVertexArrays", None, integer, pointer)(1, c.byref(vao))
        bind(gl, "glBindVertexArray", None, uint)(vao)
        bind(gl, "glGenBuffers", None, integer, pointer)(1, c.byref(vbo))
        bind(gl, "glBindBuffer", None, uint, uint)(0x8892, vbo)
        vertices = (number * 27)(-1,-1,0,0,0,.5,1,.5,1, 3,-1,0,0,0,.5,1,.5,1, -1,3,0,0,0,.5,1,.5,1)
        bind(gl, "glBufferData", None, uint, c.c_ssize_t, pointer, uint)(0x8892, c.sizeof(vertices), vertices, 0x88E4)
        for index, size, offset in [(0,3,0), (1,2,12), (2,4,20)]:
            bind(gl, "glEnableVertexAttribArray", None, uint)(index)
            bind(gl, "glVertexAttribPointer", None, uint, integer, uint, c.c_ubyte, integer, pointer)(index, size, 0x1406, 0, 36, offset)
        bind(gl, "glGenTextures", None, integer, pointer)(1, c.byref(texture))
        bind(gl, "glBindTexture", None, uint, uint)(0x0DE1, texture)
        bind(gl, "glTexParameteri", None, uint, uint, integer)(0x0DE1, 0x2801, 0x2601)
        color = (c.c_ubyte * 4)(3, 3, 8, 255)
        bind(gl, "glTexImage2D", None, uint, integer, integer, integer, integer, integer, uint, uint, pointer)(0x0DE1, 0, 0x8058, 1, 1, 0, 0x1908, 0x1401, color)
        bind(gl, "glViewport", None, integer, integer, integer, integer)(0, 0, 16, 16)
        def draw():
            bind(gl, "glDrawArrays", None, uint, integer, integer)(0x0004, 0, 3)
            result = (c.c_ubyte * 4)()
            bind(gl, "glReadPixels", None, integer, integer, integer, integer, uint, uint, pointer)(8, 8, 1, 1, 0x1908, 0x1401, result)
            assert bind(gl, "glGetError", uint)() == 0, "Error de OpenGL"
            return tuple(result)
        vacuum = draw()
        assert vacuum[:3] == (3,3,8), vacuum
        scalar("AtmosphereHeight", 80)
        vector("BetaR", (.0058,.0135,.0331))
        vector("BetaM", (.003,.003,.003))
        day = draw()
        assert day[2] > day[0] and sum(day[:3]) > sum(vacuum[:3]), day
        vector("SunDirection", (0,-1,0))
        night = draw()
        assert sum(night[:3]) < sum(day[:3]), (night, day)
        print("Shader compilado/enlazado; muestras RGBA: vacío", vacuum, "día", day, "noche", night)
    finally:
        bind(egl, "eglMakeCurrent", uint, pointer, pointer, pointer, pointer)(display, None, None, None)
        bind(egl, "eglDestroyContext", uint, pointer, pointer)(display, context)
        bind(egl, "eglDestroySurface", uint, pointer, pointer)(display, surface)
        bind(egl, "eglTerminate", uint, pointer)(display)


if __name__ == "__main__":
    check()
