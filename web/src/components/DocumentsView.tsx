import { useState, useEffect, useRef } from 'react';
import { DocumentInfo, fetchDocuments, uploadDocument, deleteDocument } from '../api';

export default function DocumentsView() {
  const [documents, setDocuments] = useState<DocumentInfo[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    loadDocuments();
  }, []);

  // Poll for processing documents
  useEffect(() => {
    const hasProcessing = documents.some(d => d.status === 'processing');
    if (!hasProcessing) return;

    const timer = setInterval(loadDocuments, 5000);
    return () => clearInterval(timer);
  }, [documents]);

  const loadDocuments = async () => {
    try {
      const data = await fetchDocuments();
      setDocuments(data);
    } catch (err) {
      console.error('Failed to load documents:', err);
    }
  };

  const handleUpload = async (file: File) => {
    setUploading(true);
    try {
      const title = file.name.replace(/\.pdf$/i, '');
      await uploadDocument(file, title);
      await loadDocuments();
    } catch (err) {
      console.error('Failed to upload:', err);
    }
    setUploading(false);
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleUpload(file);
    e.target.value = '';
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files?.[0];
    if (file && file.type === 'application/pdf') {
      handleUpload(file);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteDocument(id);
      setDocuments(prev => prev.filter(d => d.id !== id));
    } catch (err) {
      console.error('Failed to delete document:', err);
    }
  };

  return (
    <div className="documents-container">
      <div className="documents-header">
        <h1>📚 Document Library</h1>
        <p>Upload PDF textbooks to build your knowledge base</p>
      </div>

      {/* Upload Zone */}
      <div
        className={`upload-zone ${isDragging ? 'dragging' : ''}`}
        onClick={() => fileInputRef.current?.click()}
        onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
      >
        <div className="upload-icon">{uploading ? '⏳' : '📤'}</div>
        <h3>{uploading ? 'Uploading...' : 'Upload a PDF'}</h3>
        <p>{uploading ? 'Please wait...' : 'Click to browse or drag and drop'}</p>
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf"
          style={{ display: 'none' }}
          onChange={handleFileSelect}
        />
      </div>

      {/* Document List */}
      <div className="document-list">
        {documents.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">📄</div>
            <p>No documents uploaded yet</p>
          </div>
        ) : (
          documents.map(doc => (
            <div key={doc.id} className="document-card">
              <div className="doc-icon">📖</div>
              <div className="doc-info">
                <div className="doc-title">{doc.title || doc.fileName}</div>
                <div className="doc-meta">
                  {doc.fileName}
                  {doc.status === 'ready' && ` · ${doc.totalPages ?? '?'} pages · ${doc.totalChunks} chunks`}
                </div>
              </div>
              <span className={`doc-status ${doc.status}`}>
                {doc.status === 'processing' && '⏳ Processing'}
                {doc.status === 'ready' && '✓ Ready'}
                {doc.status === 'failed' && '✗ Failed'}
              </span>
              <button
                className="doc-delete"
                onClick={() => handleDelete(doc.id)}
                title="Delete"
              >
                🗑
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
