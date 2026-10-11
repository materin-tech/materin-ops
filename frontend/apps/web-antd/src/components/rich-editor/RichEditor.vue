<script lang="ts" setup>
// 基于 vditor 的多功能编辑器：所见即所得(WYSIWYG) / 即时渲染(IR) / 分屏 Markdown(SV) 三模式切换
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';

import Vditor from 'vditor';
import 'vditor/dist/index.css';

const props = withDefaults(
  defineProps<{
    height?: number;
    mode?: 'ir' | 'sv' | 'wysiwyg';
    placeholder?: string;
    value?: string;
  }>(),
  { height: 280, mode: 'wysiwyg', placeholder: '', value: '' },
);

const emit = defineEmits<{ 'update:value': [value: string] }>();

const el = ref<HTMLDivElement>();
let editor: null | Vditor = null;

onMounted(() => {
  if (!el.value) return;
  editor = new Vditor(el.value, {
    height: props.height,
    mode: props.mode,
    placeholder: props.placeholder,
    value: props.value,
    cache: { enable: false },
    toolbarConfig: { pin: true },
    toolbar: [
      'headings', 'bold', 'italic', 'strike', '|',
      'list', 'ordered-list', 'check', 'outdent', 'indent', '|',
      'quote', 'code', 'inline-code', 'link', 'table', '|',
      'upload', 'line', 'undo', 'redo', '|',
      'wysiwyg', 'ir', 'sv',
    ],
    upload: {
      // 图片/附件直接上传到资源中心（/api/resource/files/upload）
      url: '/api/resource/files/upload',
      fieldName: 'file',
      multiple: false,
      format: (files: File[], responseText: string) => {
        // 后端返回 {code:0,data:{...}}，转为 vditor 需要的结构
        try {
          const res = JSON.parse(responseText);
          if (res.code === 0 && res.data) {
            return JSON.stringify({
              msg: '',
              code: 0,
              data: { errFiles: [], succMap: { [files[0]?.name ?? 'file']: `/api/resource/files/${res.data.id}/download` } },
            });
          }
        } catch {
          // fallthrough
        }
        return responseText;
      },
    },
    after: () => {
      editor?.setValue(props.value ?? '');
    },
    input: (value: string) => {
      emit('update:value', value);
    },
  });
});

watch(
  () => props.value,
  (val) => {
    if (editor && editor.getValue() !== val) {
      editor.setValue(val ?? '');
    }
  },
);

onBeforeUnmount(() => {
  editor?.destroy();
  editor = null;
});
</script>

<template>
  <div ref="el" class="rich-editor"></div>
</template>

<style scoped>
.rich-editor {
  --vditor-border-color: var(--ant-color-border, #d9d9d9);
}
</style>
