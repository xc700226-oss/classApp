package com.classapp.schedule.ui.schoolimport

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classapp.schedule.ClassApp
import com.classapp.schedule.data.importer.ParsedCourse
import com.classapp.schedule.data.importer.SchoolParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class ImportWebViewState(
    val currentUrl: String = "",
    val isLoading: Boolean = false,
    val title: String = "",
    val errorMessage: String? = null,
    val parsedCourses: List<ParsedCourse> = emptyList(),
    val showPreview: Boolean = false,
    val importStatus: ImportStatus = ImportStatus.Idle,
    val message: String = ""
)

sealed class ImportStatus {
    data object Idle : ImportStatus()
    data object Parsing : ImportStatus()
    data object Saving : ImportStatus()
    data object Done : ImportStatus()
    data class Error(val msg: String) : ImportStatus()
}

class ImportWebViewViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ClassApp).repository
    private val _state = MutableStateFlow(ImportWebViewState())
    val state: StateFlow<ImportWebViewState> = _state.asStateFlow()

    companion object {
        private const val TAG = "ImportWebView"
    }

    fun updateUrl(url: String) {
        _state.update { it.copy(currentUrl = url) }
    }

    fun updateLoading(loading: Boolean) {
        _state.update { it.copy(isLoading = loading) }
    }

    fun updateTitle(title: String) {
        _state.update { it.copy(title = title) }
    }

    fun onPageStarted(url: String) {
        _state.update { it.copy(currentUrl = url, isLoading = true) }
    }

    fun onPageFinished(url: String) {
        _state.update { it.copy(currentUrl = url, isLoading = false) }
    }

    fun onPageError(message: String) {
        _state.update { it.copy(isLoading = false, errorMessage = message) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /** Called from JavaScriptInterface with the extracted JSON array. */
    fun onCoursesExtracted(json: String) {
        Log.d(TAG, "JS注入返回原始JSON: $json")
        try {
            val courses = SchoolParser.parseJson(json)
            if (courses.isEmpty()) {
                Log.w(TAG, "JS注入解析结果为空，JSON可能是错误对象: $json")
                _state.update {
                    it.copy(
                        importStatus = ImportStatus.Error("未找到课程数据，请确认当前页面为课表页面"),
                        message = ""
                    )
                }
            } else {
                courses.forEachIndexed { i, c ->
                    Log.i(TAG, "课程[$i]: name=${c.name}, teacher=${c.teacher}, " +
                            "room=${c.classroom}, day=${c.dayOfWeek}, " +
                            "slots=${c.startSlot}-${c.endSlot}, weeks=${c.weeks}")
                }
                _state.update {
                    it.copy(
                        parsedCourses = courses,
                        showPreview = true,
                        importStatus = ImportStatus.Idle,
                        message = "共找到 ${courses.size} 门课程"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "JS注入JSON解析异常: ${e.message}")
            _state.update {
                it.copy(importStatus = ImportStatus.Error("解析失败: ${e.message}"))
            }
        }
    }

    /** Called from JavaScriptInterface with error messages. */
    fun onHtmlReceived(msg: String) {
        Log.d(TAG, "JS回调: $msg")
        val error = when {
            msg.startsWith("FETCH_ERR") -> "请求课表数据失败，请检查网络连接"
            msg.startsWith("NO_IFRAME") -> "未找到课表页面，请先登录教务系统"
            msg.startsWith("NO_KBTABLE") -> "页面中未找到课表，请确认已打开「学期理论课表」"
            msg.startsWith("NO_COURSES_IN_TABLE") -> "课表为空，未找到课程数据"
            msg.startsWith("PARSE_ERR") -> "解析课表出错: ${msg.removePrefix("PARSE_ERR: ")}"
            msg.startsWith("EX_ERR") -> "执行出错: ${msg.removePrefix("EX_ERR: ")}"
            else -> return
        }
        Log.e(TAG, "JS错误: $msg")
        _state.update {
            it.copy(importStatus = ImportStatus.Error(error))
        }
    }

    fun dismissPreview() {
        _state.update { it.copy(showPreview = false, parsedCourses = emptyList()) }
    }

    fun confirmImport() {
        val courses = _state.value.parsedCourses
        if (courses.isEmpty()) return

        _state.update { it.copy(importStatus = ImportStatus.Saving, showPreview = false) }

        viewModelScope.launch {
            try {
                // 按课程名分配颜色索引（同名课程同色）
                val colorMap = mutableMapOf<String, Int>()
                var nextColor = 0
                courses.forEach { pc ->
                    val colorIdx = colorMap.getOrPut(pc.name) { nextColor++ }
                    repository.addCourse(
                        com.classapp.schedule.data.model.Course(
                            name = pc.name,
                            teacher = pc.teacher,
                            classroom = pc.classroom,
                            dayOfWeek = pc.dayOfWeek,
                            startSlot = pc.startSlot,
                            endSlot = pc.endSlot,
                            weeks = pc.weeks,
                            colorIndex = colorIdx
                        )
                    )
                }
                _state.update {
                    it.copy(
                        importStatus = ImportStatus.Done,
                        parsedCourses = emptyList(),
                        message = "成功导入 ${courses.size} 门课程"
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(importStatus = ImportStatus.Error("保存失败: ${e.message}"))
                }
            }
        }
    }

    fun resetStatus() {
        _state.update { it.copy(importStatus = ImportStatus.Idle, message = "") }
    }
}

// ---- JavaScript injected into the WebView ----
// GET 请求 → JS 端 DOMParser 解析 #kbtable + .kbcontent → 结构化 JSON
private val EXTRACT_JS = """
(function(){
    var frame = document.querySelector('iframe:not([style*="display: none"])');
    if (!frame) { AndroidBridge.onHtml('NO_IFRAME'); return; }

    try {
        var apiUrl = frame.src;
        if (!apiUrl || apiUrl.indexOf('xskb_list.do') < 0) {
            var basePath = frame.src.substring(0, frame.src.lastIndexOf('/') + 1);
            apiUrl = basePath + 'xskb_list.do';
        }

        fetch(apiUrl, { method: 'GET', credentials: 'same-origin' })
        .then(function(r) { return r.text(); })
        .then(function(html) {
            try {
                var doc = new DOMParser().parseFromString(html, 'text/html');
                var table = doc.getElementById('kbtable');
                if (!table) {
                    AndroidBridge.onHtml('NO_KBTABLE');
                    return;
                }

                var courses = [];
                var rows = table.querySelectorAll('tr');
                for (var ri = 1; ri < rows.length; ri++) {
                    var th = rows[ri].querySelector('th');
                    if (!th) continue;

                    var thText = th.textContent || '';
                    var rowIdx = -1;
                    if (thText.indexOf('第一') >= 0) rowIdx = 0;
                    else if (thText.indexOf('第二') >= 0) rowIdx = 1;
                    else if (thText.indexOf('第三') >= 0) rowIdx = 2;
                    else if (thText.indexOf('第四') >= 0) rowIdx = 3;
                    else if (thText.indexOf('第五') >= 0) rowIdx = 4;
                    if (rowIdx < 0) continue;

                    var cells = rows[ri].querySelectorAll('td');
                    for (var ci = 0; ci < cells.length; ci++) {
                        var day = ci + 1;
                        var hiddenDivs = cells[ci].querySelectorAll('div.kbcontent');
                        for (var di = 0; di < hiddenDivs.length; di++) {
                            var div = hiddenDivs[di];
                            if (div.classList.contains('kbcontent1')) continue;
                            var inner = div.innerHTML;
                            if (!inner || inner.trim() === '&nbsp;' || inner.trim() === '') continue;

                            var sections = inner.split(/<hr\s*\/?>/i);
                            for (var si = 0; si < sections.length; si++) {
                                var sec = sections[si].trim();
                                if (!sec || sec === '&nbsp;') continue;

                                var name = ''; var teacher = ''; var classroom = '';
                                var weeks = ''; var slots = '';

                                var parts = sec.split(/<br\s*\/?>/i);
                                for (var li = 0; li < parts.length; li++) {
                                    var line = parts[li].trim();
                                    var text = line.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim();
                                    if (!text) continue;

                                    var fm = line.match(/<font[^>]*title\s*=\s*['"]([^'"]+)['"]/);
                                    if (fm) {
                                        var label = fm[1];
                                        if (label === '老师') teacher = text;
                                        else if (label === '教室') classroom = text;
                                        else if (label === '周次(节次)') {
                                            var wm = text.match(/([\d,\-]+)\(周\)/);
                                            if (wm) weeks = wm[1];
                                            var sm = text.match(/(\[\d+-\d+节?\])/);
                                            if (sm) slots = sm[1];
                                        }
                                    } else if (/^\d{6}[-–]/.test(text)) {
                                        // skip course code
                                    } else if (!name) {
                                        name = text;
                                    }
                                }
                                if (name) {
                                    courses.push({name:name, teacher:teacher, classroom:classroom, weeks:weeks, slots:slots, day:day, row:rowIdx});
                                }
                            }
                        }
                    }
                }
                if (courses.length > 0) {
                    AndroidBridge.onCourses(JSON.stringify(courses));
                } else {
                    AndroidBridge.onHtml('NO_COURSES_IN_TABLE');
                }
            } catch(e) {
                AndroidBridge.onHtml('PARSE_ERR: ' + e.message);
            }
        })
        .catch(function(err) { AndroidBridge.onHtml('FETCH_ERR: ' + err.message); });
    } catch(e) {
        AndroidBridge.onHtml('EX_ERR: ' + e.message);
    }
})();
""".trimIndent()

/** Bridge: receives JS result into Kotlin. */
class ImportJsBridge(private val viewModel: ImportWebViewViewModel) {
    @JavascriptInterface
    fun onCourses(json: String) {
        viewModel.onCoursesExtracted(json)
    }

    @JavascriptInterface
    fun onHtml(html: String) {
        viewModel.onHtmlReceived(html)
    }
}

// ---- Composable ----

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ImportWebViewScreen(
    onBack: () -> Unit,
    viewModel: ImportWebViewViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("https://wwebvpn.sdjtu.edu.cn/") }

    // Create WebView at composable level so both bottomBar and content can access it
    val webView = remember {

        WebView(context).apply outer@ {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                allowFileAccess = true
                allowContentAccess = true
            }

            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = false

            // Enable cookie handling for session-based login
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAcceptThirdPartyCookies(this@outer, true)
                }
            }

            addJavascriptInterface(ImportJsBridge(viewModel), "AndroidBridge")

            // 自动加载教务系统 WebVPN
            loadUrl("https://wwebvpn.sdjtu.edu.cn/")

            @Suppress("DEPRECATION")
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    url?.let { viewModel.onPageStarted(it) }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let { viewModel.onPageFinished(it) }
                }

                override fun onReceivedSslError(
                    view: WebView?, handler: SslErrorHandler?, error: SslError?
                ) {
                    handler?.proceed() // Accept for school intranet
                }

                override fun onReceivedError(
                    view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        val desc = error?.description?.toString() ?: "未知错误"
                        viewModel.onPageError("加载失败: $desc")
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onReceivedError(
                    view: WebView?, errorCode: Int, description: String?, failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    val desc = description ?: "未知错误"
                    viewModel.onPageError("加载失败 ($errorCode): $desc")
                }

                override fun onReceivedHttpError(
                    view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request?.isForMainFrame == true) {
                        val code = errorResponse?.statusCode ?: 0
                        val reason = errorResponse?.reasonPhrase ?: ""
                        viewModel.onPageError("服务器错误 HTTP $code $reason")
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: WebResourceRequest?
                ): Boolean {
                    return false // Let WebView handle navigation
                }
            }

            // Handle popup windows (青果系统常用弹窗显示课表)
            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    // Redirect popup to the current WebView
                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                    transport?.webView = view
                    resultMsg?.sendToTarget()
                    return true
                }
            }
        }
    }

    // Handle completion / errors
    LaunchedEffect(state.importStatus) {
        when (val s = state.importStatus) {
            is ImportStatus.Done -> {
                snackbarHost.showSnackbar(state.message)
                viewModel.resetStatus()
            }
            is ImportStatus.Error -> {
                snackbarHost.showSnackbar(s.msg)
                viewModel.resetStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("从教务系统导入", fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // URL input bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            placeholder = { Text("输入教务系统网址") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = {
                                val url = urlInput.trim()
                                if (url.isNotEmpty()) {
                                    val finalUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                                    viewModel.clearError()
                                    viewModel.updateUrl(finalUrl)
                                    webView.loadUrl(finalUrl)
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("前往")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            Log.d("ImportWebView", "用户点击解析课表按钮, 当前URL=${state.currentUrl}")
                            webView.evaluateJavascript(EXTRACT_JS, null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !state.isLoading &&
                                  state.importStatus !is ImportStatus.Saving
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("解析当前课表")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Loading overlay
            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            // Error card
            if (state.errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            state.errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("关闭")
                            }
                            Button(onClick = {
                                viewModel.clearError()
                                webView.reload()
                            }) {
                                Text("重试")
                            }
                        }
                    }
                }
            }

            // WebView
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )

            // 操作提示
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        "在下方输入教务系统网址，登录后进入「学期理论课表」页面，点击「解析当前课表」",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    // ---- Preview dialog ----
    if (state.showPreview) {
        val courses = state.parsedCourses
        AlertDialog(
            onDismissRequest = {
                if (state.importStatus !is ImportStatus.Saving) viewModel.dismissPreview()
            },
            icon = {
                if (state.importStatus is ImportStatus.Saving) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            },
            title = { Text("确认导入") },
            text = {
                Column {
                    Text("共 ${courses.size} 门课程")
                    Spacer(modifier = Modifier.height(8.dp))
                    courses.take(5).forEach { c ->
                        val day = when (c.dayOfWeek) {
                            1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
                            5 -> "周五"; 6 -> "周六"; 7 -> "周日"; else -> ""
                        }
                        Text(
                            "• ${c.name}（${day} 第${c.startSlot}-${c.endSlot}节）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (courses.size > 5) {
                        Text(
                            "... 还有 ${courses.size - 5} 门",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (state.importStatus !is ImportStatus.Saving) {
                    Button(onClick = { viewModel.confirmImport() }) {
                        Text("确认导入")
                    }
                }
            },
            dismissButton = {
                if (state.importStatus !is ImportStatus.Saving) {
                    TextButton(onClick = { viewModel.dismissPreview() }) {
                        Text("取消")
                    }
                }
            }
        )
    }
}
