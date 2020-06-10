package org.sonar.plugins.delphi.type.intrinsic;

import static org.sonar.plugins.delphi.type.DelphiPointerType.pointerTo;
import static org.sonar.plugins.delphi.type.DelphiPointerType.untypedPointer;
import static org.sonar.plugins.delphi.type.intrinsic.IntrinsicText.ANSICHAR;
import static org.sonar.plugins.delphi.type.intrinsic.IntrinsicText.WIDECHAR;

import org.sonar.plugins.delphi.type.Type.ImmutablePointerType;

public enum IntrinsicPointer {
  POINTER("Pointer", untypedPointer()),
  PWIDECHAR("PWideChar", pointerTo(WIDECHAR.type, true)),
  PANSICHAR("PAnsiChar", pointerTo(ANSICHAR.type, true)),
  PCHAR("PChar", PWIDECHAR.type);

  public final String image;
  public final ImmutablePointerType type;

  IntrinsicPointer(String image, ImmutablePointerType type) {
    this.image = image;
    this.type = type;
  }
}
