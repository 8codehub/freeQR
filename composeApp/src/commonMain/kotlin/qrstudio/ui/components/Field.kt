package qrstudio.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qrstudio.theme.LocalTokens
import androidx.compose.material3.LocalTextStyle

@Composable fun Field(label: String,content: @Composable ()->Unit) {
    val t=LocalTokens.current
    Column(Modifier.fillMaxWidth()) { Text(label,color=t.label,fontSize=12.sp);Spacer(Modifier.height(5.dp));content() }
}
@Composable fun TextInput(value: String,onChange: (String)->Unit,placeholder: String="",minLines: Int=1,minHeight: Int=44,keyboard: KeyboardType=KeyboardType.Text,tag: String="") {
    val t=LocalTokens.current;var focused by remember { mutableStateOf(false) }
    val bring=remember { BringIntoViewRequester() }
    val imeVisible=WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)>0
    val keyboardController=LocalSoftwareKeyboardController.current
    val focusManager=LocalFocusManager.current
    LaunchedEffect(focused,imeVisible) {
        if(focused && imeVisible) { delay(240);bring.bringIntoView() }
    }
    val style=LocalTextStyle.current.copy(color=t.text,fontSize=14.sp)
    BasicTextField(value,onChange,Modifier.fillMaxWidth().heightIn(min=minHeight.dp).testTag(tag).bringIntoViewRequester(bring).onFocusChanged { focused=it.isFocused }
        .background(t.surface).border(if(focused) 2.dp else 1.dp,if(focused) t.accent else t.divider).padding(horizontal=10.dp,vertical=6.dp),
        textStyle=style,cursorBrush=SolidColor(t.accent),keyboardOptions=KeyboardOptions(keyboardType=keyboard,imeAction=if(minLines==1) ImeAction.Done else ImeAction.Default),
        keyboardActions=KeyboardActions(onDone={ keyboardController?.hide();focusManager.clearFocus() }),singleLine=minLines==1,minLines=minLines,
        decorationBox={ inner -> Box { if(value.isEmpty()) Text(placeholder,style=style,color=t.muted);inner() } })
}
