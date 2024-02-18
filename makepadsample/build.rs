use std::env;

// without this configuration cargo ndk runs into the following errors:
//      ld: error: unable to find library -laaudio
//      ld: error: unable to find library -lamidi
//      ld: error: unable to find library -lnativewindow
//      ld: error: unable to find library -lcamera2ndk
//      clang-14: error: linker command failed with exit code 1 (use -v to see invocation)
// there might be a better way to resolve this, but I don't have any experience with linking

const TOOLCHAIN_VERSION: &str = "33";

fn main() {
    let target_os = env::var("CARGO_CFG_TARGET_OS").unwrap();

    if target_os == "android" {
        let target_arch = env::var("CARGO_CFG_TARGET_ARCH").unwrap();

        let lib_dir = if cfg!(target_os = "macos") {
            "toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/lib/"
        } else if cfg!(target_os = "linux") {
            "toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/"
        } else if cfg!(target_os = "windows") {
            "toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/lib/"
        } else {
            panic!("unsupported host OS")
        };

        let ndk_home = env::var("NDK_HOME").expect("NDK_HOME not set");
        if target_arch == "aarch64" {
            println!("cargo:rustc-link-search={ndk_home}/{lib_dir}aarch64-linux-android/{TOOLCHAIN_VERSION}");
        } else if target_arch == "arm" {
            println!("cargo:rustc-link-search={ndk_home}/{lib_dir}arm-linux-androideabi/{TOOLCHAIN_VERSION}");
        } else if target_arch == "i686" {
            println!("cargo:rustc-link-search={ndk_home}/{lib_dir}i686-linux-android/{TOOLCHAIN_VERSION}");
        } else if target_arch == "x86_64" {
            println!("cargo:rustc-link-search={ndk_home}/{lib_dir}x86_64-linux-android/{TOOLCHAIN_VERSION}");
        }
    }
}
