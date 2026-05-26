import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<div className="p-8 text-2xl font-bold text-blue-600">MindFit — Login (Coming Soon)</div>} />
        <Route path="/signup" element={<div className="p-8 text-2xl font-bold text-purple-600">MindFit — Signup (Coming Soon)</div>} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
